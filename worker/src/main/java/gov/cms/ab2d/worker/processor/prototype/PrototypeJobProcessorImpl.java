package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import gov.cms.ab2d.worker.processor.ProgressTracker;
import gov.cms.ab2d.worker.processor.SerializedEobs;
import gov.cms.ab2d.worker.processor.prototype.lease.JobLeaseRepository;
import gov.cms.ab2d.worker.processor.prototype.lease.PrototypeFenceGuard;
import gov.cms.ab2d.worker.processor.prototype.lease.PrototypeJobLeaseRenewer;
import gov.cms.ab2d.worker.service.JobChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PROD_LIST;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_COMPLETED;
import static gov.cms.ab2d.job.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.job.model.JobStatus.FAILED;
import static gov.cms.ab2d.job.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.job.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.job.model.JobStatus.SUCCESSFUL;

/**
 * prototype implementation of the pause/resume processor.
 */
@Slf4j
@Component
public class PrototypeJobProcessorImpl implements PrototypeJobProcessor {
    static final String PROTOTYPE_JOB_NAME = "ab2dPrototypeJob";
    static final String MANAGER_STEP_NAME = "ab2dPrototypePartitionManagerStep";
    static final String WORKER_STEP_NAME = "ab2dPrototypeWorkerStep";
    static final String JOB_UUID_PARAM = "jobUuid";
    // the fence token allows this worker to prove ownership of a job. The token
    // is used to namespace outputs so that different workers cannot corrupt the work
    // of other workers.
    static final String FENCE_TOKEN_PARAM = "fenceToken";

    // IO exceptions from contacting BFD are worth retrying
    private static final List<Class<? extends Throwable>> TRANSIENT_EXCEPTIONS = List.of(
            IOException.class,
            SocketTimeoutException.class,
            ConnectException.class,
            ResourceAccessException.class,
            HttpServerErrorException.class);

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final org.springframework.batch.core.repository.JobRepository batchJobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CoverageV3Service coverageV3Service;
    private final PrototypeBatchMetadataRepository batchMeta;
    private final PrototypeOutputAssembler outputAssembler;
    private final PrototypeJobRecovery recovery;
    private final PrototypeJobLeaseRenewer leaseRenewer;
    private final JobLeaseRepository jobLease;
    // progress and failure reporting, reused from the main processor
    private final JobChannelService jobChannelService;
    private final JobProgressService jobProgressService;
    private final JobProgressUpdateService jobProgressUpdateService;
    private final SQSEventClient eventLogger;
    // per-JVM diagnostic identity recorded on the lease so logs/monitoring can attribute ownership
    private final String owner = "worker-" + java.util.UUID.randomUUID();
    private final PrototypeProperties props;

    private final int failureThreshold;
    private final int auditFilesTtlHours;

    private final BeneficiaryItemReader beneficiaryItemReader;
    private final EobItemProcessor eobItemProcessor;
    private final ItemStreamWriter<SerializedEobs> ndjsonItemWriter;

    public PrototypeJobProcessorImpl(
            JobRepository jobRepository,
            JobOperator jobOperator,
            org.springframework.batch.core.repository.JobRepository batchJobRepository,
            PlatformTransactionManager transactionManager,
            CoverageV3Service coverageV3Service,
            PrototypeBatchMetadataRepository batchMeta,
            PrototypeOutputAssembler outputAssembler,
            PrototypeJobRecovery recovery,
            PrototypeJobLeaseRenewer leaseRenewer,
            JobLeaseRepository jobLease,
            JobChannelService jobChannelService,
            JobProgressService jobProgressService,
            JobProgressUpdateService jobProgressUpdateService,
            SQSEventClient eventLogger,
            BeneficiaryItemReader beneficiaryItemReader,
            EobItemProcessor eobItemProcessor,
            ItemStreamWriter<SerializedEobs> ndjsonItemWriter,
            PrototypeProperties props,
            @Value("${failure.threshold}") int failureThreshold,
            @Value("${audit.files.ttl.hours}") int auditFilesTtlHours) {
        this.jobRepository = jobRepository;
        this.jobOperator = jobOperator;
        this.batchJobRepository = batchJobRepository;
        this.transactionManager = transactionManager;
        this.coverageV3Service = coverageV3Service;
        this.batchMeta = batchMeta;
        this.outputAssembler = outputAssembler;
        this.recovery = recovery;
        this.leaseRenewer = leaseRenewer;
        this.jobLease = jobLease;
        this.jobChannelService = jobChannelService;
        this.jobProgressService = jobProgressService;
        this.jobProgressUpdateService = jobProgressUpdateService;
        this.eventLogger = eventLogger;
        this.beneficiaryItemReader = beneficiaryItemReader;
        this.eobItemProcessor = eobItemProcessor;
        this.ndjsonItemWriter = ndjsonItemWriter;
        this.props = props;
        this.failureThreshold = failureThreshold;
        this.auditFilesTtlHours = auditFilesTtlHours;
    }

    @Override
    public Job process(String jobUuid) {
        Job job = jobRepository.findByJobUuid(jobUuid);
        if (job == null) {
            throw new IllegalArgumentException("Job " + jobUuid + " was not found");
        }

        // prototype scope is V3 only
        if (job.getFhirVersion() != FhirVersion.R4V3) {
            job.setStatus(FAILED);
            job.setStatusMessage("Rejected due to version (not v3)");
            return jobRepository.save(job);
        }

        log.info("Prototype is handling job {}", jobUuid);

        job.setStatus(IN_PROGRESS);
        job.setStatusMessage("Processing via prototype");
        job = jobRepository.save(job);

        String contractNumber = job.getContractNumber();

        // Either resume gracefully, by taking over the current execution, or hard restart, by
        // bumping the fence token and healing the execution.
        PrototypeJobRecovery.Ownership ownership = recovery.acquire(jobUuid, owner);
        long fenceToken = ownership.fenceToken();
        boolean softResume = ownership.softResume();

        // Start renewing the heartbeat
        leaseRenewer.track(jobUuid, fenceToken);
        try {
            // jobUuid identifies the instance
            JobParameters parameters = new JobParametersBuilder()
                    .addString(JOB_UUID_PARAM, jobUuid)
                    .addLong(FENCE_TOKEN_PARAM, fenceToken, false)
                    .toJobParameters();

            // Only build the aggregated attribution table on a fresh start
            JobExecution last = batchJobRepository.getLastJobExecution(PROTOTYPE_JOB_NAME, parameters);
            if (last == null) {
                log.info("no prior batch execution for job {} - creating aggregated attribution table", jobUuid);
                coverageV3Service.createAggregatedAttributionTable(contractNumber);
            } else if (!coverageV3Service.aggregatedTableExists(contractNumber)) {
                // A prior worker that failed terminally or was fenced out may have dropped the aggregated
                // table. We safely remake it because a hard recovery re-runs the partitioner from scratch anyway.
                log.warn("prior batch execution {} for job {} but aggregated table for contract {} is missing - "
                        + "rebuilding ({} at token {})",
                        last.getId(), jobUuid, contractNumber, softResume ? "soft resume" : "hard recovery", fenceToken);
                coverageV3Service.createAggregatedAttributionTable(contractNumber);
            } else {
                log.info("prior batch execution {} for job {} - {} at token {} (reusing aggregated table)",
                        last.getId(), jobUuid, softResume ? "soft resume" : "hard recovery", fenceToken);
            }

            // job progress is initialized on startup since we could be restarting
            // a job that was already in progress
            jobProgressUpdateService.initJob(jobUuid);
            jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, failureThreshold);
            jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_EXPECTED,
                    coverageV3Service.getMaxRowNumber(contractNumber));
            long alreadyProcessed = batchMeta.completedProcessedCount(jobUuid, WORKER_STEP_NAME);
            if (alreadyProcessed > 0) {
                jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_PROCESSED, alreadyProcessed);
            }

            org.springframework.batch.core.job.Job batchJob = buildPartitionedJob(contractNumber, jobUuid, fenceToken);

            JobExecution execution = launchOrResume(batchJob, parameters, last);
            log.info("prototype job {} finished with status {}", jobUuid, execution.getStatus());

            // If we don't have the highest token anymore, someone else owns the job now
            // so we just quit. Any partition we had in progress is going to be overwritten/restarted anyway.
            // There is no shutdown work to do, the other worker that owns the job now will be doing any
            // necessary cleanup
            if (wasFencedOut(jobUuid, fenceToken)) {
                log.info("prototype job {} was superseded (ran under token {}, newer token now holds the lease) - "
                        + "exiting quietly with no resubmit", jobUuid, fenceToken);
                return jobRepository.findByJobUuid(jobUuid);
            }

            // update job object if we're shutting down
            Job current = jobRepository.findByJobUuid(jobUuid);
            if (current != null && current.getStatus() == CANCELLED) {
                log.warn("prototype job {} was cancelled during processing; leaving CANCELLED and cleaning up", jobUuid);
                coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
                outputAssembler.deleteJobDirectory(jobUuid);
                return current;
            }

            if (execution.getStatus() == BatchStatus.COMPLETED) {
                if (thresholdExceeded(jobUuid)) {
                    // The batch finished but too many beneficiaries errored, this is a terminal failure
                    applyThresholdFailure(job, contractNumber, jobUuid);
                } else {
                    // Failure during assembly is terminal and fails the job. We can recover from a crash
                    // during assembly, but not from an exception e.g. missing files in a "completed" job.
                    outputAssembler.assemble(job, jobUuid, contractNumber);
                    completeJobSuccessfully(job, jobUuid);
                    coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
                }
            } else if (execution.getStatus() == BatchStatus.STOPPED || wasInterrupted(execution)) {
                // Graceful shutdown ensures that the batch job is fully stopped and every file has been closed
                // and fsynced its file to the saved offset.
                // The last thing we do is mark the job as suspended. If we crash here, we'll just hard-recover
                // anyway, which is unfortunate, but will avoid corruption/mistakes.
                jobLease.markCleanSuspend(jobUuid, fenceToken);
                job.setStatus(SUBMITTED);
                job.setStatusMessage("Paused via prototype");
            } else if (hasThresholdException(execution) || thresholdExceeded(jobUuid)) {
                // Too many beneficiaries errored, terminal failure
                applyThresholdFailure(job, contractNumber, jobUuid);
            } else {
                // A run that failed for other reasons is resumable, up to some tolerance for retrying
                applyFailureOutcome(job, contractNumber, jobUuid,
                        "Prototype failed with status " + execution.getStatus());
            }
        } catch (Exception e) {
            // If we lost the lease, a newer owner is (or will be) running this job. Do not fail the job or
            // delete the aggregated table out from under them - that would corrupt the live run and, since
            // last != null on any later recovery, could leave the job permanently unable to complete.
            // Exit quietly and let the current owner finish.
            if (wasFencedOut(jobUuid, fenceToken)) {
                log.info("prototype job {} threw while superseded (ran under token {}, newer token now holds the "
                        + "lease) - exiting quietly, the current owner is responsible for this job", jobUuid, fenceToken);
                return jobRepository.findByJobUuid(jobUuid);
            }
            // issues with launching are terminal and fail the job without retry
            log.error("prototype job {} failed to launch", jobUuid, e);
            job.setStatus(FAILED);
            job.setStatusMessage("Prototype execution failed: " + e.getMessage());
            coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
        } finally {
            leaseRenewer.untrack(jobUuid);
        }

        preserveLiveProgress(job, jobUuid);

        Job result = jobRepository.save(job);
        // EFS cleanup. We do not handle in_progress or submitted status here, because both of those
        // are resumable and somebody else will handle the files/directories
        if (result.getStatus() == SUCCESSFUL) {
            outputAssembler.deleteIntermediateDirectories(jobUuid);
        } else if (result.getStatus() == FAILED) {
            outputAssembler.deleteJobDirectory(jobUuid);
        }
        return result;
    }

    /**
     * Increment the jobs failed attempts, then send it back to be resubmitted
     */
    private void applyFailureOutcome(Job job, String contractNumber, String jobUuid, String message) {
        int failures = batchMeta.failedExecutionCount(jobUuid);
        int maxFailureAttempts = props.getMaxFailureAttempts();
        if (failures < maxFailureAttempts) {
            job.setStatus(SUBMITTED);
            job.setStatusMessage(message + " (attempt " + failures + " of " + maxFailureAttempts + "; will resume)");
            log.warn("prototype job {} failed ({}/{} attempts) - resubmitting for resume", jobUuid, failures, maxFailureAttempts);
        } else {
            job.setStatus(FAILED);
            job.setStatusMessage(message + " (failed after " + failures + " attempts)");
            log.error("prototype job {} exhausted {} failure attempts - marking FAILED", jobUuid, maxFailureAttempts);
            coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
        }
    }

    /**
     * Terminal failure because too many beneficiaries errored
     */
    private void applyThresholdFailure(Job job, String contractNumber, String jobUuid) {
        log.error("prototype job {} failed: too many patient records in the job had failures", jobUuid);
        job.setStatus(FAILED);
        job.setStatusMessage("Failed: too many patient records in the job had failures");
        coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
    }

    /**
     * Refreshes the jobs progress before the job saves so we don't overwrite the progress count
     * with stale info
     */
    private void preserveLiveProgress(Job job, String jobUuid) {
        if (job.getStatus() == SUCCESSFUL) {
            return;
        }
        Job persisted = jobRepository.findByJobUuid(jobUuid);
        if (persisted != null) {
            job.setProgress(persisted.getProgress());
        }
    }

    /**
     * Record terminal success state, great job.
     */
    private void completeJobSuccessfully(Job job, String jobUuid) {
        ProgressTracker tracker = jobProgressService.getStatus(jobUuid);
        int processed = tracker == null ? 0 : tracker.getPatientRequestProcessedCount();
        String message = String.format("%s via prototype: processed %d patients into %d file(s)",
                EOB_JOB_COMPLETED, processed, job.getJobOutputs().size());
        eventLogger.logAndAlert(job.buildJobStatusChangeEvent(SUCCESSFUL, message), PROD_LIST);

        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setProgress(100);
        job.setExpiresAt(OffsetDateTime.now().plusHours(auditFilesTtlHours));
        job.setCompletedAt(OffsetDateTime.now());
    }

    /** True if the progress tracker shows the failed-bene ratio at or above the threshold. */
    private boolean thresholdExceeded(String jobUuid) {
        ProgressTracker tracker = jobProgressService.getStatus(jobUuid);
        return tracker != null && tracker.getTotalCount() > 0 && tracker.isErrorThresholdExceeded();
    }

    /** True if the batch failed because a chunk tripped the failure threshold mid-run. */
    private boolean hasThresholdException(JobExecution execution) {
        return execution.getAllFailureExceptions().stream()
                .anyMatch(ThresholdExceededException.class::isInstance);
    }

    /**
     * Signal every running step execution to stop at the next chunk boundary and wait for them
     * to drain.
     *
     * If we don't drain in time, we don't mark the job as suspended, so it will be hard-recovered.
     */
    @Override
    public void stopForShutdown() {
        Set<JobExecution> running = batchJobRepository.findRunningJobExecutions(PROTOTYPE_JOB_NAME);
        if (running.isEmpty()) {
            return;
        }
        log.info("shutdown: stopping {} running prototype batch execution(s) before releasing jobs", running.size());
        for (JobExecution je : running) {
            try {
                jobOperator.stop(je);
            } catch (Exception e) {
                log.warn("shutdown: failed to signal stop for batch execution {}", je.getId(), e);
            }
        }

        // Wait for the partition threads to actually finish before changing status
        // might need a TODO for a more robust system than a sleep
        long shutdownAwaitMs = props.getShutdownAwaitMs();
        long deadline = System.currentTimeMillis() + shutdownAwaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (batchJobRepository.findRunningJobExecutions(PROTOTYPE_JOB_NAME).isEmpty()) {
                log.info("shutdown: all prototype batch executions stopped");
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("shutdown: prototype batch executions still running after {}ms; proceeding with status reset anyway",
                shutdownAwaitMs);
    }

    /**
     * Start a fresh batch execution, or resume a prior one for the same jobUuid.
     * Spring Batch will restart a job if the same jobUuid is submitted.
     */
    private JobExecution launchOrResume(org.springframework.batch.core.job.Job batchJob, JobParameters parameters,
            JobExecution last) throws Exception {
        if (last == null) {
            log.info("no prior batch execution found - starting fresh");
            return jobOperator.start(batchJob, parameters);
        }

        if (last.getStatus() == BatchStatus.COMPLETED) {
            log.info("prior batch execution {} already COMPLETED - nothing to resume", last.getId());
            return last;
        }

        // Hard crash recovery already handles this, so normally this will do nothing
        // because there will be nothing left to recover. In the soft-resume case, there will usually
        // be nothing to recover either, since we suspend cleanly, but this covers any edge cases
        if (last.getStatus().isRunning() || hasRunningStepExecution(last)) {
            log.info("recovering stale batch execution {} (job status {}) before resume", last.getId(), last.getStatus());
            jobOperator.recover(last);
        }

        // resume is based on jobuuid
        log.info("resuming from prior batch execution {} (status {})", last.getId(), last.getStatus());
        return jobOperator.start(batchJob, parameters);
    }

    /**
     * True if the execution failed because it was interrupted
     */
    private boolean wasInterrupted(JobExecution execution) {
        return execution.getAllFailureExceptions().stream()
                .anyMatch(InterruptedException.class::isInstance);
    }

    /**
     * If our token is lower than the current token in the DB, someone else took over our job, probably for
     * a good reason. Only should happen when this worker misses its heartbeat repeatedly.
     */
    private boolean wasFencedOut(String jobUuid, long ranUnderToken) {
        return jobLease.currentToken(jobUuid).map(current -> current > ranUnderToken).orElse(false);
    }

    /**
     * True if any exec step of the given job execution is still in a running state
     * makes sure we can avoid a situation where a "running" step blocks restarting
     */
    private boolean hasRunningStepExecution(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .anyMatch(stepExecution -> stepExecution.getStatus().isRunning());
    }

    /**
     * build the partitioned batch job for a contract
     */
    private org.springframework.batch.core.job.Job buildPartitionedJob(String contractNumber, String jobUuid,
            long fenceToken) {
        var workerStepBuilder = new StepBuilder(WORKER_STEP_NAME, batchJobRepository)
                .<CoverageSummary, SerializedEobs>chunk(props.getChunkSize())
                .reader(beneficiaryItemReader)
                .processor(eobItemProcessor)
                .writer(ndjsonItemWriter)
                // each chunk commit comes with a fence token which prevents
                // us from committing work if we've lost the job
                .listener(new PrototypeFenceGuard(jobLease, jobUuid, fenceToken))
                // per-chunk progress and failure reporting
                .listener(new PrototypeProgressListener(jobChannelService, jobProgressService, jobUuid))
                .faultTolerant()
                // retry items that fail, skip benes that fail too much
                .retryLimit(props.getItemRetryLimit())
                .skipPolicy(new PrototypeSkipPolicy())
                // count each skipped beneficiary as one error against the error threshold
                .skipListener(new PrototypeSkipCounter(jobChannelService, jobUuid));
        TRANSIENT_EXCEPTIONS.forEach(workerStepBuilder::retry);

        Step workerStep = workerStepBuilder
                // abort the step at the next chunk boundary if the job is cancelled mid-run
                .listener(new JobCancellationChunkListener(jobRepository, jobUuid))
                .allowStartIfComplete(false)
                // should basically never trip
                .startLimit(props.getMaxStartAttempts())
                .transactionManager(transactionManager)
                .build();

        BeneficiaryPartitioner partitioner =
                new BeneficiaryPartitioner(coverageV3Service, contractNumber, props.getPartitionSize());

        // the manager partitions the work, and each partition gets its own
        // workerStep, which brings along its own reader/processor/writer
        // and writes to its own ndjson file
        Step managerStep = new StepBuilder(MANAGER_STEP_NAME, batchJobRepository)
                .partitioner(WORKER_STEP_NAME, partitioner)
                .step(workerStep)
                .taskExecutor(partitionTaskExecutor())
                .gridSize(props.getConcurrency())
                .build();

        return new JobBuilder(PROTOTYPE_JOB_NAME, batchJobRepository)
                .start(managerStep)
                .build();
    }

    /**
     * Executor for the partitioned worker steps
     * Async allows for concurrent work
     */
    private TaskExecutor partitionTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("proto-partition-");
        executor.setConcurrencyLimit(Math.max(1, props.getConcurrency()));
        return executor;
    }

}
