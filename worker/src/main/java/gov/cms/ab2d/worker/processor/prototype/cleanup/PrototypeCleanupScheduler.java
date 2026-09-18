package gov.cms.ab2d.worker.processor.prototype.cleanup;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.worker.processor.prototype.PrototypeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_PROTOTYPE_ENABLED;
import static gov.cms.ab2d.worker.processor.prototype.PrototypeJobProcessorImpl.PROTOTYPE_JOB_NAME;

/**
 * Runs batch metadata cleanup on a schedule, every 6 hours by default. If there is a job running, cleanup
 * is skipped until the next scheduled time.
 */
@Slf4j
@Component
public class PrototypeCleanupScheduler {

    public static final String CLEANUP_JOB_NAME = "ab2dPrototypeCleanupJob";
    static final String CLEANUP_STEP_NAME = "ab2dPrototypeCleanupStep";
    static final String SWEEP_ID_PARAM = "sweepId";
    static final String RETENTION_DAYS_PARAM = "retentionDays";

    private final JobOperator jobOperator;
    private final JobRepository batchJobRepository;
    private final PropertiesService propertiesService;
    private final PrototypeProperties props;

    private final Job cleanupJob;

    public PrototypeCleanupScheduler(JobOperator jobOperator, JobRepository batchJobRepository,
                                     PlatformTransactionManager transactionManager,
                                     PrototypeCleanupTasklet cleanupTasklet,
                                     PropertiesService propertiesService, PrototypeProperties props) {
        this.jobOperator = jobOperator;
        this.batchJobRepository = batchJobRepository;
        this.propertiesService = propertiesService;
        this.props = props;
        this.cleanupJob = buildCleanupJob(batchJobRepository, transactionManager, cleanupTasklet);
    }

    @Scheduled(fixedDelayString = "${pause-resume.prototype.cleanup-interval:6h}")
    public void sweepBatchHistory() {
        if (!props.isCleanupEnabled() || !propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)) {
            return;
        }

        try {
            int running = batchJobRepository.findRunningJobExecutions(PROTOTYPE_JOB_NAME).size();
            if (running > 0) {
                log.debug("skipping prototype cleanup due to running jobs");
                return;
            }
            launchSweep();
        } catch (Exception e) {
            log.error("prototype cleanup sweep failed", e);
        }
    }

    /**
     * The cleanup is a batch job so it has an ID.
     * TODO: Possible improvement would be to make the sweep ID param nonrandom.
     * That way, workers would be able to coordinate cleanup without needing to talk to each other.
     * Example: The sweep ID is just today's date. It will only get run once per day, and spring batch will
     * recognize resubmission of the task with the same name and not run it again.
     */
    private void launchSweep() throws Exception {
        jobOperator.start(cleanupJob, new JobParametersBuilder()
                .addString(SWEEP_ID_PARAM, UUID.randomUUID().toString())
                .addLong(RETENTION_DAYS_PARAM, (long) props.getCleanupRetentionDays(), false)
                .toJobParameters());
    }

    private static Job buildCleanupJob(JobRepository batchJobRepository,
                                       PlatformTransactionManager transactionManager,
                                       PrototypeCleanupTasklet cleanupTasklet) {
        return new JobBuilder(CLEANUP_JOB_NAME, batchJobRepository)
                .start(new StepBuilder(CLEANUP_STEP_NAME, batchJobRepository)
                        .tasklet(cleanupTasklet, transactionManager)
                        .build())
                .build();
    }
}
