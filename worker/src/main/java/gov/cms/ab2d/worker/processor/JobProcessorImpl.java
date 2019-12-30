package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.PatientClaimsProcessor;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyStaticImports")
public class JobProcessorImpl implements JobProcessor {
    private static final String OUTPUT_FILE_SUFFIX = ".ndjson";
    private static final String ERROR_FILE_SUFFIX = "_error.ndjson";

    @Value("${cancellation.check.frequency:10}")
    private int cancellationCheckFrequency;

    @Value("${efs.mount}")
    private String efsMount;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final BeneficiaryAdapter beneficiaryAdapter;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final OptOutRepository optOutRepository;


    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job process(final String jobUuid) {

        final Job job = jobRepository.findByJobUuid(jobUuid);
        log.info("Found job");

        final List<Contract> attestedContracts = getAttestedContracts(job);
        log.info("Job [{}] has [{}] attested contracts", jobUuid, attestedContracts.size());

        try {
            final Path outputDirPath = Paths.get(efsMount, jobUuid);
            final Path outputDir = fileService.createDirectory(outputDirPath);

            for (Contract contract : attestedContracts) {
                log.info("Job [{}] - contract [{}] ", jobUuid, contract.getContractNumber());

                var jobOutputs = processContract(outputDir, contract, jobUuid);

                jobOutputs.forEach(jobOutput -> job.addJobOutput(jobOutput));
                jobOutputRepository.saveAll(jobOutputs);
            }

            completeJob(job);

        } catch (JobCancelledException e) {
            log.warn("Job: [{}] CANCELLED", jobUuid);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Job: [{}] FAILED", jobUuid);
        }

        return job;
    }

    private List<Contract> getAttestedContracts(Job job) {

        // when the job is submitted for a specific contract, process the export for only that contract.
        final Contract jobSpecificContract = job.getContract();
        if (jobSpecificContract != null && jobSpecificContract.getAttestedOn() != null) {
            log.info("Job [{}] submitted for a specific attested contract [{}] ", jobSpecificContract.getContractNumber());
            return Collections.singletonList(jobSpecificContract);
        }

        //Job does not specify a contract.
        //Get the aggregated attested Contracts for the sponsor & process all of them
        final Sponsor sponsor = job.getUser().getSponsor();
        final List<Contract> attestedContracts = sponsor.getAggregatedAttestedContracts();

        log.info("Job [{}] has [{}] attested contracts", job.getJobUuid(), attestedContracts.size());
        return attestedContracts;
    }

    private List<JobOutput> processContract(final Path outputDir, Contract contract, String jobUuid) {
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contract.getContractName()));

        var contractNumber = contract.getContractNumber();
        var outputFile = fileService.createFile(outputDir, contractNumber + OUTPUT_FILE_SUFFIX);
        var errorFile = fileService.createFile(outputDir, contractNumber + ERROR_FILE_SUFFIX);

        var patientsByContract = beneficiaryAdapter.getPatientsByContract(contractNumber);
        var patients = patientsByContract.getPatients();
        int patientCount = patients.size();

        // A mutex lock that all threads for a contract uses while writing into the shared files
        var lock = new ReentrantLock();

        int errorCount = 0;
        JobStatus jobStatus = null;

        int recordsProcessedCount = 0;
        final List<Future<Integer>> futureHandles = new ArrayList<>();
        for (GetPatientsByContractResponse.PatientDTO patient : patients) {
            ++recordsProcessedCount;

            final String patientId = patient.getPatientId();

            if (isOptOutPatient(patientId)) {
                // this patient has opted out. skip patient record.
                continue;
            }

            futureHandles.add(patientClaimsProcessor.process(patientId, lock, outputFile, errorFile));

            if (recordsProcessedCount % cancellationCheckFrequency == 0) {
                errorCount += processHandles(futureHandles);

                // A Job could run for a long time, perhaps hours.
                // While the job is in progress, the job could be cancelled.
                // So the worker needs to periodically check the job status to ensure it has not been cancelled.

                jobStatus = jobRepository.findJobStatus(jobUuid);
                if (jobHasBeenCancelled(jobStatus)) {
                    log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ", jobUuid);
                    break;
                }
            }
        }

        if (jobHasBeenCancelled(jobStatus)) {
            final String errMsg = "Job was cancelled while it was being processed";
            log.warn("{} - JobUuid :[{}]", errMsg, jobUuid);

            // cancel any outstanding futures that have not started processing.
            futureHandles.parallelStream().forEach(future -> future.cancel(false));

            //At this point, there may be a few futures that are already in progress.
            //But all the futures that are not yet in progress would be cancelled.

            throw new JobCancelledException(errMsg);
        }

        while (!futureHandles.isEmpty()) {
            sleep();
            errorCount += processHandles(futureHandles);
        }


        final List<JobOutput> jobOutputs = new ArrayList<>();
        if (errorCount < patientCount) {
            jobOutputs.add(createJobOutput(outputFile, false));
        }
        if (errorCount > 0) {
            log.warn("Encountered {} errors during job processing", errorCount);
            jobOutputs.add(createJobOutput(errorFile, true));
        }

        return jobOutputs;
    }

    private boolean jobHasBeenCancelled(JobStatus jobStatus) {
        return CANCELLED.equals(jobStatus);
    }


    private boolean isOptOutPatient(String patientId) {

        final List<OptOut> optOuts = optOutRepository.findByHicn(patientId);
        if (optOuts.isEmpty()) {
            // No opt-out record found for this patient - Opt-In by default.
            return false;
        }

        // opt-out record has an effective date.
        // if any of the opt-out records for a patient is effective as of today or earlier, the patient has opted-out
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        return optOuts.stream()
                .anyMatch(optOut -> optOut.getEffectiveDate().isBefore(tomorrow));
    }

    private int processHandles(List<Future<Integer>> futureHandles) {
        int errorCount = 0;

        var iterator = futureHandles.iterator();
        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                try {
                    var responseCount = future.get();
                    if (responseCount > 0) {
                        errorCount += responseCount;
                    }
                } catch (InterruptedException e) {
                    final String errMsg = "interrupted exception while processing patient ";
                    log.error(errMsg);
                    throw new RuntimeException(errMsg, e);
                } catch (ExecutionException e) {
                    final String errMsg = "exception while processing patient ";
                    log.error(errMsg);
                    throw new RuntimeException(errMsg, e.getCause());
                } catch (CancellationException e) {
                    // This could happen in the rare event that a job was cancelled mid-process.
                    // due to which the futures in the queue (that were not yet in progress) were cancelled.
                    // Nothing to be done here
                    log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
                }
                iterator.remove();
            }
        }

        return errorCount;
    }


    private JobOutput createJobOutput(Path outputFile, boolean isError) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(outputFile.getFileName().toString());
        jobOutput.setFhirResourceType("ExplanationOfBenefits");
        jobOutput.setError(isError);
        return jobOutput;
    }

    private void completeJob(Job job) {
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setExpiresAt(OffsetDateTime.now().plusDays(1));
        job.setCompletedAt(OffsetDateTime.now());

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getJobUuid());
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.warn("interrupted exception in thread.sleep(). Ignoring");
        }
    }

}
