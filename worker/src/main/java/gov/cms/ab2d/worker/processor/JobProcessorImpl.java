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
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.util.stream.Collectors;

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
    private static final int SLEEP_DURATION = 250;
    private static final long ROLL_OVER_THRESHOLD = 200; // 200 MB
    private static final int ONE_MEGA_BYTE = 1024 * 1024;


    @Value("${cancellation.check.frequency:10}")
    private int cancellationCheckFrequency;

    @Value("${report.progress.db.frequency:100}")
    private int reportProgressDbFrequency;

    @Value("${report.progress.log.frequency:100}")
    private int reportProgressLogFrequency;

    @Value("${file.try.lock.timeout}")
    private int tryLockTimeout;

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
            processJob(job, outputDirPath);

        } catch (JobCancelledException e) {
            log.warn("Job: [{}] CANCELLED", jobUuid);

            log.info("Deleting output directory : {} ", outputDirPath.toAbsolutePath());
            deleteExistingDirectory(outputDirPath);

        } catch (Exception e) {
            log.error("Unexpected expection ", e);
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Job: [{}] FAILED", jobUuid);
        }

        return job;
    }

    private void processJob(Job job, Path outputDirPath) {
        var outputDir = createOutputDirectory(outputDirPath);

        var attestedContracts = getAttestedContracts(job);
        var jobUuid = job.getJobUuid();
        var progressTracker = initializeProgressTracker(jobUuid, attestedContracts);

        for (Contract contract : attestedContracts) {
            log.info("Job [{}] - contract [{}] ", jobUuid, contract.getContractNumber());

            JobDataWriter writer = new JobDataWriterImpl(outputDir, contract, tryLockTimeout, getRollOverThreshold());
            var contractData = new ContractData(writer, contract, progressTracker, contract.getAttestedOn());

            var jobOutputs = processContract(contractData);
            jobOutputs.forEach(jobOutput -> job.addJobOutput(jobOutput));
            jobOutputRepository.saveAll(jobOutputs);
        }

        completeJob(job);
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

        log.info("Created job output directory: {}", directory.toAbsolutePath());
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


    /**
     * iterates through each contract,
     * calls the contract Adapter
     * to make a list of contracts to process.
     *
     * @param attestedContracts
     * @return
     */
    private ProgressTracker initializeProgressTracker(String jobUuid, List<Contract> attestedContracts) {
        return ProgressTracker.builder()
                .jobUuid(jobUuid)
                .patientsByContracts(fetchPatientsForAllContracts(attestedContracts))
                .build();
    }

    private List<GetPatientsByContractResponse> fetchPatientsForAllContracts(List<Contract> attestedContracts) {
        return attestedContracts
                .stream()
                .map(contract -> contract.getContractNumber())
                .map(contractNumber -> contractAdapter.getPatients(contractNumber))
                .collect(Collectors.toList());
    }

    private long getRollOverThreshold() {
        return ROLL_OVER_THRESHOLD * ONE_MEGA_BYTE;
    }

    private List<JobOutput> processContract(ContractData contractData) {
        var contract = contractData.getContract();
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contract.getContractName()));

        var contractNumber = contract.getContractNumber();

        var progressTracker = contractData.getProgressTracker();

        var patientsByContract = getPatientsByContract(contractNumber, progressTracker);
        var patients = patientsByContract.getPatients();
        int patientCount = patients.size();
        log.info("Contract [{}] has [{}] Patients", contractNumber, patientCount);

        boolean isCancelled = false;

        int recordsProcessedCount = 0;
        var futureHandles = new ArrayList<Future<Void>>();
        for (PatientDTO patient : patients) {
            ++recordsProcessedCount;

            final String patientId = patient.getPatientId();

            if (isOptOutPatient(patientId)) {
                // this patient has opted out. skip patient record.
                continue;
            }

            futureHandles.add(patientClaimsProcessor.process(patient, contractData.getWriter(), contract.getAttestedOn()));

            if (recordsProcessedCount % cancellationCheckFrequency == 0) {

                var jobUuid = contractData.getProgressTracker().getJobUuid();
                isCancelled = hasJobBeenCancelled(jobUuid);
                if (isCancelled) {
                    log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ", jobUuid);
                    cancelFuturesInQueue(futureHandles);
                    break;
                }

                processHandles(futureHandles, progressTracker);
            }
        }

        while (!futureHandles.isEmpty()) {
            sleep();
            processHandles(futureHandles, progressTracker);
        }

        if (isCancelled) {
            final String errMsg = "Job was cancelled while it was being processed";
            log.warn("{}", errMsg);
            throw new JobCancelledException(errMsg);
        }

        return createJobOutputs(contractData.getWriter());
    }


    private GetPatientsByContractResponse getPatientsByContract(String contractNumber, ProgressTracker progressTracker) {
        return progressTracker.getPatientsByContracts()
                .stream()
                .filter(byContract -> byContract.getContractNumber().equals(contractNumber))
                .findFirst()
                .get();
    }


    private void cancelFuturesInQueue(List<Future<Void>> futureHandles) {

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

        final List<OptOut> optOuts = optOutRepository.findByCcwId(patientId);
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

    private void processHandles(List<Future<Void>> futureHandles, ProgressTracker progressTracker) {
        var iterator = futureHandles.iterator();
        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                try {
                    future.get();
                    progressTracker.incrementProcessedCount();
                } catch (InterruptedException e) {
                    cancelFuturesInQueue(futureHandles);
                    log.error("interrupted exception while processing patient", e);

                    final String errMsg = ExceptionUtils.getRootCauseMessage(e);
                    throw new RuntimeException(errMsg, ExceptionUtils.getRootCause(e));

                } catch (ExecutionException e) {
                    cancelFuturesInQueue(futureHandles);
                    log.error("exception while processing patient ", e);

                    final String errMsg = ExceptionUtils.getRootCauseMessage(e);
                    throw new RuntimeException(errMsg, ExceptionUtils.getRootCause(e));

                } catch (CancellationException e) {
                    // This could happen in the rare event that a job was cancelled mid-process.
                    // due to which the futures in the queue (that were not yet in progress) were cancelled.
                    // Nothing to be done here
                    log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
                }
                iterator.remove();
            }
        }

        trackProgress(progressTracker);
    }


    private void trackProgress(ProgressTracker progressTracker) {
        if (progressTracker.isTimeToUpdateDatabase(reportProgressDbFrequency)) {
            final int percentageCompleted = progressTracker.getPercentageCompleted();

            if (percentageCompleted > progressTracker.getLastUpdatedPercentage()) {
                jobRepository.updatePercentageCompleted(progressTracker.getJobUuid(), percentageCompleted);
                progressTracker.setLastUpdatedPercentage(percentageCompleted);
            }
        }

        var processedCount = progressTracker.getProcessedCount();
        if (progressTracker.isTimeToLog(reportProgressLogFrequency)) {
            progressTracker.setLastLogUpdateCount(processedCount);

            var totalCount = progressTracker.getTotalCount();
            var percentageCompleted = progressTracker.getPercentageCompleted();
            log.info("[{}/{}] records processed = [{}% completed]", processedCount, totalCount, percentageCompleted);
        }
    }


    private List<JobOutput> createJobOutputs(final JobDataWriter jobDataWriter) {
        final List<JobOutput> jobOutputs = new ArrayList<>();

        // create Job Output records for data files
        final List<JobOutput> dataJobOutputs = jobDataWriter.getDataFiles().stream()
                .map(dataFile -> createJobOutput(dataFile, false))
                .collect(Collectors.toList());
        jobOutputs.addAll(dataJobOutputs);

        // create Job Output record for error file
        final List<JobOutput> errorJobOutputs = jobDataWriter.getErrorFiles().stream()
                .map(errorFile -> createJobOutput(errorFile, true))
                .collect(Collectors.toList());
        jobOutputs.addAll(errorJobOutputs);

        if (jobOutputs.isEmpty()) {
            var errMsg = "The export process has produced no results";
            throw new RuntimeException(errMsg);
        }

        return jobOutputs;
    }


    private JobOutput createJobOutput(Path outputFile, boolean isError) {
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
            Thread.sleep(SLEEP_DURATION);
        } catch (InterruptedException e) {
            log.warn("interrupted exception in thread.sleep(). Ignoring");
        }
    }

}
