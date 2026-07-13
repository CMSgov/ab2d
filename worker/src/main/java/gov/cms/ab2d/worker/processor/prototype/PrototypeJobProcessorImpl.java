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

import java.util.List;
import java.util.Optional;
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
    static final int MAX_RESTART_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final org.springframework.batch.core.repository.JobRepository batchJobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CoverageV3Service coverageV3Service;
    private final int partitionSize;
    private final int chunkSize;
    private final int concurrency;

    private final BeneficiaryItemReader beneficiaryItemReader;
    private final EobItemProcessor eobItemProcessor;
    private final ItemStreamWriter<List<IBaseResource>> ndjsonItemWriter;

    public PrototypeJobProcessorImpl(
            JobRepository jobRepository,
            JobOperator jobOperator,
            org.springframework.batch.core.repository.JobRepository batchJobRepository,
            PlatformTransactionManager transactionManager,
            CoverageV3Service coverageV3Service,
            BeneficiaryItemReader beneficiaryItemReader,
            EobItemProcessor eobItemProcessor,
            ItemStreamWriter<List<IBaseResource>> ndjsonItemWriter,
            @Value("${pause-resume.prototype.partition-size:1000}") int partitionSize,
            @Value("${pause-resume.prototype.chunk-size:100}") int chunkSize,
            @Value("${pause-resume.prototype.concurrency:4}") int concurrency) {
        this.jobRepository = jobRepository;
        this.jobOperator = jobOperator;
        this.batchJobRepository = batchJobRepository;
        this.transactionManager = transactionManager;
        this.coverageV3Service = coverageV3Service;
        this.partitionSize = partitionSize;
        this.beneficiaryItemReader = beneficiaryItemReader;
        this.eobItemProcessor = eobItemProcessor;
        this.ndjsonItemWriter = ndjsonItemWriter;
        this.chunkSize = chunkSize;
        this.concurrency = concurrency;
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

        // table not created yet due to bypass
        coverageV3Service.createAggregatedAttributionTable(contractNumber);

        org.springframework.batch.core.job.Job batchJob = buildPartitionedJob(contractNumber);

        // jobUuid is the identifying parameter
        JobParameters parameters = new JobParametersBuilder()
                .addString(JOB_UUID_PARAM, jobUuid)
                .toJobParameters();

        try {
            JobExecution execution = launchOrResume(batchJob, parameters);
            log.info("prototype job {} finished with status {}", jobUuid, execution.getStatus());

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
                job.setStatus(FAILED);
                job.setStatusMessage("Prototype failed with status " + execution.getStatus());
            }
        } catch (Exception e) {
                log.error("prototype job {} failed to launch", jobUuid, e);
                job.setStatus(FAILED);
                job.setStatusMessage("Prototype execution failed: " + e.getMessage());
        }

        return jobRepository.save(job);
    }

    /**
     * Start a fresh batch execution, or resume a prior one for the same jobUuid.
     * Spring Batch will restart a job if the same jobUuid is submitted.
     */
    private JobExecution launchOrResume(org.springframework.batch.core.job.Job batchJob, JobParameters parameters)
            throws Exception {
        JobExecution last = batchJobRepository.getLastJobExecution(PROTOTYPE_JOB_NAME, parameters);

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
    private org.springframework.batch.core.job.Job buildPartitionedJob(String contractNumber) {
        Step workerStep = new StepBuilder(WORKER_STEP_NAME, batchJobRepository)
                .<CoverageSummary, List<IBaseResource>>chunk(chunkSize)
                .reader(beneficiaryItemReader)
                .processor(eobItemProcessor)
                .writer(ndjsonItemWriter)
                .allowStartIfComplete(false)
                .startLimit(MAX_RESTART_ATTEMPTS)
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
