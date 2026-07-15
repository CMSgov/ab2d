package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private final int partitionSize;
    private final int chunkSize;
    private final int concurrency;
    private final int maxFailureAttempts;
    private final int maxStartAttempts;
    private final int itemRetryLimit;
    private final long shutdownAwaitMs;

    private final BeneficiaryItemReader beneficiaryItemReader;
    private final EobItemProcessor eobItemProcessor;
    private final ItemStreamWriter<List<IBaseResource>> ndjsonItemWriter;

    public PrototypeJobProcessorImpl(
            JobRepository jobRepository,
            JobOperator jobOperator,
            org.springframework.batch.core.repository.JobRepository batchJobRepository,
            PlatformTransactionManager transactionManager,
            CoverageV3Service coverageV3Service,
            PrototypeBatchMetadataRepository batchMeta,
            BeneficiaryItemReader beneficiaryItemReader,
            EobItemProcessor eobItemProcessor,
            ItemStreamWriter<List<IBaseResource>> ndjsonItemWriter,
            @Value("${pause-resume.prototype.partition-size:1000}") int partitionSize,
            @Value("${pause-resume.prototype.chunk-size:100}") int chunkSize,
            @Value("${pause-resume.prototype.concurrency:4}") int concurrency,
            @Value("${pause-resume.prototype.max-failure-attempts:3}") int maxFailureAttempts,
            @Value("${pause-resume.prototype.max-start-attempts:50}") int maxStartAttempts,
            @Value("${pause-resume.prototype.item-retry-limit:3}") int itemRetryLimit,
            @Value("${pause-resume.prototype.shutdown-await-ms:30000}") long shutdownAwaitMs) {
        this.jobRepository = jobRepository;
        this.jobOperator = jobOperator;
        this.batchJobRepository = batchJobRepository;
        this.transactionManager = transactionManager;
        this.coverageV3Service = coverageV3Service;
        this.batchMeta = batchMeta;
        this.partitionSize = partitionSize;
        this.beneficiaryItemReader = beneficiaryItemReader;
        this.eobItemProcessor = eobItemProcessor;
        this.ndjsonItemWriter = ndjsonItemWriter;
        this.chunkSize = chunkSize;
        this.concurrency = concurrency;
        this.maxFailureAttempts = maxFailureAttempts;
        this.maxStartAttempts = maxStartAttempts;
        this.itemRetryLimit = itemRetryLimit;
        this.shutdownAwaitMs = shutdownAwaitMs;
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

        // jobUuid is the identifying parameter
        JobParameters parameters = new JobParametersBuilder()
                .addString(JOB_UUID_PARAM, jobUuid)
                .toJobParameters();

        // Only build the aggregated attribution table on a fresh start
        JobExecution last = batchJobRepository.getLastJobExecution(PROTOTYPE_JOB_NAME, parameters);
        if (last == null) {
            log.info("no prior batch execution for job {} - creating aggregated attribution table", jobUuid);
            coverageV3Service.createAggregatedAttributionTable(contractNumber);
        } else {
            log.info("prior batch execution {} for job {} - reusing existing aggregated table", last.getId(), jobUuid);
        }

        org.springframework.batch.core.job.Job batchJob = buildPartitionedJob(contractNumber, jobUuid);

        try {
            JobExecution execution = launchOrResume(batchJob, parameters, last);
            log.info("prototype job {} finished with status {}", jobUuid, execution.getStatus());

            // update job object if we're shutting down
            Job current = jobRepository.findByJobUuid(jobUuid);
            if (current != null && current.getStatus() == CANCELLED) {
                log.warn("prototype job {} was cancelled during processing; leaving CANCELLED and cleaning up", jobUuid);
                coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
                return current;
            }

            if (execution.getStatus() == BatchStatus.COMPLETED) {
                job.setStatus(SUCCESSFUL);
                job.setStatusMessage("Completed via prototype");
                coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
            } else if (execution.getStatus() == BatchStatus.STOPPED || wasInterrupted(execution)) {
                // this is sort of redundant since the shutdown hook also sets job status to
                // submitted, but it's helpful to do it here too so there's no gaps
                job.setStatus(SUBMITTED);
                job.setStatusMessage("Paused via prototype");
            } else {
                // A run that failed is resumable, up to some tolerance for retrying
                applyFailureOutcome(job, contractNumber, jobUuid,
                        "Prototype failed with status " + execution.getStatus());
            }
        } catch (Exception e) {
            // issues with launching are terminal and fail the job without retry
            log.error("prototype job {} failed to launch", jobUuid, e);
            job.setStatus(FAILED);
            job.setStatusMessage("Prototype execution failed: " + e.getMessage());
            coverageV3Service.deleteAggregatedTableForContract(contractNumber, Optional.of(jobUuid));
        }

        return jobRepository.save(job);
    }

    /**
     * Increment the jobs failed attempts, then send it back to be resubmitted
     */
    private void applyFailureOutcome(Job job, String contractNumber, String jobUuid, String message) {
        int failures = batchMeta.failedExecutionCount(jobUuid);
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

        // Worker crashing/stopping can leave orphaned steps
        // recover any orphaned steps before attempting to resume a job
        // otherwise it might think the job is already running
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
    private org.springframework.batch.core.job.Job buildPartitionedJob(String contractNumber, String jobUuid) {
        var workerStepBuilder = new StepBuilder(WORKER_STEP_NAME, batchJobRepository)
                .<CoverageSummary, List<IBaseResource>>chunk(chunkSize)
                .reader(beneficiaryItemReader)
                .processor(eobItemProcessor)
                .writer(ndjsonItemWriter)
                // retry transient item level failures before failing the chunk
                .faultTolerant()
                .retryLimit(itemRetryLimit);
        TRANSIENT_EXCEPTIONS.forEach(workerStepBuilder::retry);

        Step workerStep = workerStepBuilder
                // abort the step at the next chunk boundary if the job is cancelled mid-run
                .listener(new JobCancellationChunkListener(jobRepository, jobUuid))
                .allowStartIfComplete(false)
                // should basically never trip
                .startLimit(maxStartAttempts)
                .transactionManager(transactionManager)
                .build();

        BeneficiaryPartitioner partitioner = new BeneficiaryPartitioner(coverageV3Service, contractNumber, partitionSize);

        // the manager partitions the work, and each partition gets its own
        // workerStep, which brings along its own reader/processor/writer
        // and writes to its own ndjson file
        Step managerStep = new StepBuilder(MANAGER_STEP_NAME, batchJobRepository)
                .partitioner(WORKER_STEP_NAME, partitioner)
                .step(workerStep)
                .taskExecutor(partitionTaskExecutor())
                .gridSize(concurrency)
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
        executor.setConcurrencyLimit(Math.max(1, concurrency));
        return executor;
    }

}
