package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.aggregator.FileOutputType;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ContractSearchEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.service.JobChannelService;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PROD_LIST;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_CALL_FAILURE;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_COMPLETED;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_FAILURE;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_QUEUE_MISMATCH;
import static gov.cms.ab2d.job.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.job.model.JobStatus.FAILED;
import static gov.cms.ab2d.job.model.JobStatus.SUCCESSFUL;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S2142") //java:S2142: "InterruptedException" should not be ignored
public class JobProcessorImpl implements JobProcessor {

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${audit.files.ttl.hours}")
    private int auditFilesTTLHours;

    /**
     * Failure threshold an integer expressed as a percentage of failure tolerated in a batch
     **/
    @Value("${failure.threshold}")
    private int failureThreshold;

    private final FileService fileService;
    private final JobChannelService jobChannelService;
    private final JobProgressService jobProgressService;
    private final JobProgressUpdateService jobProgressUpdateService;

    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final ContractProcessor contractProcessor;
    private final SQSEventClient eventLogger;

    /**
     * Load the job and process it
     *
     * @param jobUuid - the job id of the job to process
     * @return the processed job
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job process(final String jobUuid) {

        // Load the job
        final Job job = jobRepository.findByJobUuid(jobUuid);
        log.info("Found job");

        // Determine the output directory based on the job id
        Path outputDirPath = null;
        try {
            outputDirPath = Paths.get(efsMount, jobUuid);
            processJob(job, outputDirPath);

        } catch (JobCancelledException e) {
            job.setStatus(CANCELLED);
            log.info("Job: [{}] CANCELLED", jobUuid);

            if (outputDirPath != null) {
                log.info("Deleting output directory : {} ", outputDirPath.toAbsolutePath());
                deleteExistingDirectory(outputDirPath, job);
            }
        } catch (Exception e) {

            String contract = job.getContractNumber();
            String message;
            // Says this is always false but that isn't true
            if (e instanceof PSQLException) {
                message = "internal server error";
                log.error("major database exception", e);
            } else {
                message = String.format("Job %s failed for contract #%s because %s", jobUuid, contract, e.getMessage());
            }

            // Log exception to relevant loggers
            eventLogger.logAndAlert(job.buildJobStatusChangeEvent(FAILED, EOB_JOB_FAILURE + " " + message), PUBLIC_LIST);
            log.error("Unexpected exception executing job {}", e.getMessage());

            // Update database status
            job.setStatus(FAILED);
            job.setStatusMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            log.info("Job: [{}] FAILED", jobUuid);
            jobRepository.save(job);
        }

        return job;
    }

    /**
     * Process in individual contract
     *
     * @param job             - the job in which the contract belongs
     * @param outputDirPath   - the location of the job output
     * @throws ExecutionException   when there is an issue with searching
     * @throws InterruptedException - when the search is interrupted
     */
    void processContract(Job job, Path outputDirPath)
            throws ExecutionException, InterruptedException {
        log.info("Job [{}] - contract [{}] ", job.getJobUuid(), job.getContractNumber());

        try {
            // Retrieve the contract beneficiaries
            var jobOutputs = contractProcessor.process(job);

            // For each job output, add to the job and save the result
            jobOutputs.forEach(job::addJobOutput);
            jobOutputRepository.saveAll(jobOutputs);

            // If the job is done searching
            verifyTrackedJobProgress(job);
        } finally {
            // Guarantee that we write out statistics on the job if possible
            persistTrackedJobProgress(job);
        }
    }

    void verifyTrackedJobProgress(Job job) {
        ProgressTracker progressTracker = jobProgressService.getStatus(job.getJobUuid());

        if (progressTracker == null) {
            log.info("Job [{}] - contract [{}] does not have any progress information, skipping verifying tracker",
                    job.getJobUuid(), job.getContractNumber());
            return;
        }
        //Ignore for S4802 during Centene support
        if (job.getContractNumber().equals("S4802") || job.getContractNumber().equals("Z1001")) {
            return;
        }
        // Number in database
        int expectedPatients = progressTracker.getPatientsExpected();

        // Number queued to retrieve
        int queuedPatients = progressTracker.getPatientRequestQueuedCount();

        // Number of retrievals processed
        int processedPatients = progressTracker.getPatientRequestProcessedCount();

        if (expectedPatients != queuedPatients) {
            String alertMessage = String.format(EOB_JOB_QUEUE_MISMATCH + " [%s] expected beneficiaries (%d) does not match queued beneficiaries (%d)",
                    job.getJobUuid(), expectedPatients, queuedPatients);
            log.error(alertMessage);
            eventLogger.alert(alertMessage, PROD_LIST);
        }

        if (expectedPatients != processedPatients) {
            String alertMessage = String.format(EOB_JOB_CALL_FAILURE + " [%s] expected beneficiaries (%d) does not match processed beneficiaries (%d)",
                    job.getJobUuid(), expectedPatients, queuedPatients);
            log.error(alertMessage);
            eventLogger.alert(alertMessage, PROD_LIST);
        }
    }

    void persistTrackedJobProgress(Job job) {
        ProgressTracker progressTracker = jobProgressService.getStatus(job.getJobUuid());

        if (progressTracker == null) {
            log.info("Job [{}] - contract [{}] does not have any progress information, skipping persisting tracker",
                    job.getJobUuid(), job.getContractNumber());
            return;
        }

        int eobFilesCreated = progressTracker.getPatientFailureCount() == 0 ? job.getJobOutputs().size()
                : job.getJobOutputs().size() - 1;

        // Regardless of whether we pass or fail the basic
        eventLogger.sendLogs(new ContractSearchEvent(job.getOrganization(),
                job.getJobUuid(),
                job.getContractNumber(),
                progressTracker.getPatientsExpected(),
                progressTracker.getPatientRequestQueuedCount(),
                progressTracker.getPatientRequestProcessedCount(),
                progressTracker.getPatientFailureCount(),
                progressTracker.getPatientsWithEobsCount(),
                progressTracker.getEobsFetchedCount(),
                progressTracker.getEobsProcessedCount(),
                eobFilesCreated
        ));
    }

    /**
     * Process the Job and put the contents into the output directory
     *
     * @param job           - the job to process
     * @param outputDirPath - the output directory to put all the files
     */
    private void processJob(Job job, Path outputDirPath) throws ExecutionException, InterruptedException {
        // Create the output directory
        createOutputDirectory(outputDirPath, job);

        // start a progress tracker
        jobProgressUpdateService.initJob(job.getJobUuid());     // A hack since everything runs in the worker JVM
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.FAILURE_THRESHHOLD, failureThreshold);

        try {
            processContract(job, outputDirPath);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("Having issue retrieving patients for contract " + job.getContractNumber());
            throw ex;
        }

        completeJob(job);
    }

    /**
     * Given a path to a directory, create it. If it already exists, delete it and its contents and recreate it
     *
     * @param outputDirPath - the path to the output directory you want to create
     */
    private void createOutputDirectory(Path outputDirPath, Job job) {
        Path directory;
        try {
            directory = fileService.createDirectory(outputDirPath);
        } catch (UncheckedIOException e) {
            final IOException cause = e.getCause();
            if (cause != null && cause.getMessage().equalsIgnoreCase("Directory already exists")) {
                log.warn("Directory already exists. Delete and create afresh ...");
                deleteExistingDirectory(outputDirPath, job);
                directory = fileService.createDirectory(outputDirPath);
            } else {
                throw e;
            }
        }

        log.info("Created job output directory: {}", directory.toAbsolutePath());
    }

    /**
     * Delete directory with all the ndjson files. If the files are directories or symbolic links, write
     * error, but continue. If it's a regular file, delete it, then delete the directory. If the directory is not
     * empty, throws an exception (if it has files other than ndjson)
     *
     * @param outputDirPath - the directory to delete
     */
    private void deleteExistingDirectory(Path outputDirPath, Job job) {
        final File[] files = outputDirPath.toFile().listFiles();

        assert files != null;
        // First find all the subdirectories and go down them recursively
        for (File file : files) {
            if (file.isDirectory()) {
                deleteExistingDirectory(Path.of(file.getAbsolutePath()), job);
            }
        }

        // Now find all the matching files in the directory and delete them
        final File[] matchingFiles = outputDirPath.toFile().listFiles(getFilenameFilter());
        for (File file : matchingFiles) {
            final Path filePath = file.toPath();
            if (Files.isRegularFile(filePath)) {
                eventLogger.sendLogs(job.buildFileEvent(filePath.toFile(), FileEvent.FileStatus.DELETE));
                doDelete(filePath);
            }
        }

        // Now delete the current directory
        doDelete(outputDirPath);
    }

    /**
     * @return a Filename filter for ndjson
     */
    FilenameFilter getFilenameFilter() {
        return (dir, name) -> {
            final String filename = name.toLowerCase();
            for (FileOutputType type : FileOutputType.values()) {
                if (type != FileOutputType.UNKNOWN && filename.endsWith(type.getSuffix())) {
                    return true;
                }
            }
            return false;
        };
    }

    private void doDelete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ex) {
            var errMsg = "Could not delete ";
            log.error("{} : {} ", errMsg, path.toAbsolutePath());
            throw new UncheckedIOException(errMsg + path.toFile().getName(), ex);
        }
    }

    /**
     * Set the job as complete in the database
     *
     * @param job - The job to set as complete
     */
    private void completeJob(Job job) {
        ProgressTracker progressTracker = jobProgressService.getStatus(job.getJobUuid());
        String jobFinishedMessage = String.format(EOB_JOB_COMPLETED + " ContractWorkerDto %s processed " +
                "%d patients generating %d eobs and %d files (including the error file if any)",
                job.getContractNumber(), progressTracker.getPatientRequestProcessedCount(),
                progressTracker.getEobsProcessedCount(),
                job.getJobOutputs().size());

        // In all environments log to database and or Kinesis
        // In prod additionally log to Slack as an alert
        JobStatusChangeEvent statusEvent = job.buildJobStatusChangeEvent(SUCCESSFUL, jobFinishedMessage);
        eventLogger.logAndAlert(statusEvent, PROD_LIST);

        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setProgress(100);
        job.setExpiresAt(OffsetDateTime.now().plusHours(auditFilesTTLHours));
        job.setCompletedAt(OffsetDateTime.now());

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getJobUuid());
    }
}
