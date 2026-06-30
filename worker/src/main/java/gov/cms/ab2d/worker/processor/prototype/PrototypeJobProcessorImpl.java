package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import static gov.cms.ab2d.job.model.JobStatus.FAILED;
import static gov.cms.ab2d.job.model.JobStatus.IN_PROGRESS;
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

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final org.springframework.batch.core.repository.JobRepository batchJobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CoverageV3Service coverageV3Service;
    private final int pageSize;
    private final long partitionDelayMs;

    public PrototypeJobProcessorImpl(
            JobRepository jobRepository,
            JobOperator jobOperator,
            org.springframework.batch.core.repository.JobRepository batchJobRepository,
            PlatformTransactionManager transactionManager,
            CoverageV3Service coverageV3Service,
            @Value("${eob.job.patient.queue.page.size}") int pageSize,
            @Value("${pause-resume.prototype.partition-delay-ms:0}") long partitionDelayMs) {
        this.jobRepository = jobRepository;
        this.jobOperator = jobOperator;
        this.batchJobRepository = batchJobRepository;
        this.transactionManager = transactionManager;
        this.coverageV3Service = coverageV3Service;
        this.pageSize = pageSize;
        this.partitionDelayMs = partitionDelayMs;
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
            JobExecution execution = jobOperator.start(batchJob, parameters);
            log.info("prototype job {} finished with status {}",
                    jobUuid, execution.getStatus());

            if (execution.getStatus() == BatchStatus.COMPLETED) {
                job.setStatus(SUCCESSFUL);
                job.setStatusMessage("Completed via prototype");
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
     * build the partitioned batch job for a contract
     */
    private org.springframework.batch.core.job.Job buildPartitionedJob(String contractNumber) {
        Step workerStep = new StepBuilder(WORKER_STEP_NAME, batchJobRepository)
                .tasklet(partitionTasklet(), transactionManager)
                .build();

        BeneficiaryPartitioner partitioner = new BeneficiaryPartitioner(coverageV3Service, contractNumber, pageSize);

        Step managerStep = new StepBuilder(MANAGER_STEP_NAME, batchJobRepository)
                .partitioner(WORKER_STEP_NAME, partitioner)
                .step(workerStep)
                .gridSize(1)
                .build();

        return new JobBuilder(PROTOTYPE_JOB_NAME, batchJobRepository)
                .start(managerStep)
                .build();
    }

    /**
     * stub tasklet, replace with reader/writer
     */
    private Tasklet partitionTasklet() {
        return (contribution, chunkContext) -> {
            ExecutionContext ec = contribution.getStepExecution().getExecutionContext();
            int partitionIndex = ec.getInt(BeneficiaryPartitioner.KEY_PARTITION_INDEX);
            long startRow = ec.getLong(BeneficiaryPartitioner.KEY_START_ROW);
            long endRow = ec.getLong(BeneficiaryPartitioner.KEY_END_ROW);
            int benes = ec.getInt(BeneficiaryPartitioner.KEY_BENES);
            String contract = ec.getString(BeneficiaryPartitioner.KEY_CONTRACT);

            log.info("processing partition {}: rows {},{}", partitionIndex, startRow, endRow);

            // to test crashing
            if (partitionDelayMs > 0) {
                Thread.sleep(partitionDelayMs);
            }

            // TODO real V3 EOB processing
            return RepeatStatus.FINISHED;
        };
    }
}
