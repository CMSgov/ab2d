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
import gov.cms.ab2d.worker.adapter.bluebutton.ContractAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.PatientClaimsProcessor;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
import static gov.cms.ab2d.common.util.Constants.EOB;
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
    private final ContractAdapter contractAdapter;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final OptOutRepository optOutRepository;


    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job process(final String jobUuid) {

        final Job job = jobRepository.findByJobUuid(jobUuid);
        log.info("Found job");

        Path outputDirPath = null;
        try {
            outputDirPath = Paths.get(efsMount, jobUuid);
            final Path outputDir = createOutputDirectory(outputDirPath);

            final List<Contract> attestedContracts = getAttestedContracts(job);
            for (Contract contract : attestedContracts) {
                log.info("Job [{}] - contract [{}] ", jobUuid, contract.getContractNumber());

                var jobOutputs = processContract(outputDir, contract, jobUuid);

                jobOutputs.forEach(jobOutput -> job.addJobOutput(jobOutput));
                jobOutputRepository.saveAll(jobOutputs);
            }

            completeJob(job);

        } catch (JobCancelledException e) {
            log.warn("Job: [{}] CANCELLED", jobUuid);

            log.info("Deleting output directory : {} ", outputDirPath.toAbsolutePath());
            deleteExistingDirectory(outputDirPath);

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Job: [{}] FAILED", jobUuid);
        }

        return job;
    }

    private Path createOutputDirectory(Path outputDirPath) {
        Path directory = null;
        try {
            directory = fileService.createDirectory(outputDirPath);
        } catch (UncheckedIOException e) {
            final IOException cause = e.getCause();
            if (cause != null && cause.getMessage().equalsIgnoreCase("Directory already exists")) {
                log.warn("Directory already exists. Delete and create afresh ...");
                deleteExistingDirectory(outputDirPath);
                directory = fileService.createDirectory(outputDirPath);
            } else {
                throw e;
            }
        }
        return directory;
    }

    private void deleteExistingDirectory(Path outputDirPath) {
        final File[] files = outputDirPath.toFile()
                .listFiles((dir, name) -> name.toLowerCase().endsWith(OUTPUT_FILE_SUFFIX));

        for (File file : files) {
            final Path filePath = file.toPath();
            if (file.isDirectory() || Files.isSymbolicLink(filePath)) {
                var errMsg = "File is not a regular file";
                log.error("{} - isDirectory: {}", errMsg, file.isDirectory());
                continue;
            }

            if (Files.isRegularFile(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException ex) {
                    var errMsg = "Could not delete file ";
                    log.error("{} : {}", errMsg, filePath.toAbsolutePath());
                    throw new UncheckedIOException(errMsg + filePath.toFile().getName(), ex);
                }
            }
        }

        try {
            Files.delete(outputDirPath);
        } catch (IOException ex) {
            var errMsg = "Could not delete directory ";
            log.error("{} : {} ", errMsg, outputDirPath.toAbsolutePath());
            throw new UncheckedIOException(errMsg + outputDirPath.toFile().getName(), ex);
        }
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
        var outputFile = fileService.createOrReplaceFile(outputDir, contractNumber + OUTPUT_FILE_SUFFIX);
        var errorFile = fileService.createOrReplaceFile(outputDir, contractNumber + ERROR_FILE_SUFFIX);

        var patientsByContract = contractAdapter.getPatients(contractNumber);
        var patients = patientsByContract.getPatients();
        int patientCount = patients.size();

        // A mutex lock that all threads for a contract uses while writing into the shared files
        var lock = new ReentrantLock();

        int errorCount = 0;
        boolean isCancelled = false;

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

                isCancelled = hasJobBeenCancelled(jobUuid);
                if (isCancelled) {
                    log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ", jobUuid);
                    cancelFuturesInQueue(futureHandles);
                    break;
                }
            }
        }

        while (!futureHandles.isEmpty()) {
            sleep();
            errorCount += processHandles(futureHandles);
        }

        if (isCancelled) {
            final String errMsg = "Job was cancelled while it was being processed";
            log.warn("{}", errMsg);
            throw new JobCancelledException(errMsg);
        }

        final List<JobOutput> jobOutputs = new ArrayList<>();
        if (errorCount < patientCount) {
            Job job = jobRepository.findByJobUuid(jobUuid);
            jobOutputs.add(createJobOutput(outputFile, false, job));
        }
        if (errorCount > 0) {
            log.warn("Encountered {} errors during job processing", errorCount);
            Job job = jobRepository.findByJobUuid(jobUuid);
            jobOutputs.add(createJobOutput(errorFile, true, job));
        }

        return jobOutputs;
    }

    private void cancelFuturesInQueue(List<Future<Integer>> futureHandles) {

        // cancel any futures that have not started processing and are waiting in the queue.
        futureHandles.parallelStream().forEach(future -> future.cancel(false));

        //At this point, there may be a few futures that are already in progress.
        //But all the futures that are not yet in progress would be cancelled.
    }

    /**
     * A Job could run for a long time, perhaps hours.
     * While the job is in progress, the job could be cancelled.
     * So the worker needs to periodically check the job status to ensure it has not been cancelled.
     *
     * @param jobUuid
     * @return
     */
    private boolean hasJobBeenCancelled(String jobUuid) {
        final JobStatus jobStatus = jobRepository.findJobStatus(jobUuid);
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
                    log.error(errMsg, e);
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


    private JobOutput createJobOutput(Path outputFile, boolean isError, Job job) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(outputFile.getFileName().toString());
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setError(isError);
        return jobOutput;
    }

    private void completeJob(Job job) {
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setProgress(100);
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
