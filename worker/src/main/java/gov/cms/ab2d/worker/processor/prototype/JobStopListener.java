package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.processor.SerializedEobs;
import gov.cms.ab2d.worker.processor.prototype.lease.JobLeaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.Optional;

/**
 * Polls the AB2D job status once per chunk and, if the job has been cancelled or
 * received a request to pause, it prevents new chunks from running

 */
@Slf4j
public class JobStopListener implements ItemWriteListener<SerializedEobs>, StepExecutionListener {

    private final JobRepository jobRepository;
    private final JobLeaseRepository jobLease;
    private final String jobUuid;

    public JobStopListener(JobRepository jobRepository, JobLeaseRepository jobLease, String jobUuid) {
        this.jobRepository = jobRepository;
        this.jobLease = jobLease;
        this.jobUuid = jobUuid;
    }

    /**
     * Decline a partition that has not started yet
     */
    @Override
    public void beforeStep(@NonNull StepExecution stepExecution) {
        if (stepExecution.isTerminateOnly()) {
            // Shutdown stops the whole job execution, so it's already handled
            return;
        }
        stopReason().ifPresent(reason -> {
            log.warn("job {} {}, so partition {} will not start", jobUuid, reason, stepExecution.getStepName());
            stepExecution.setTerminateOnly();
        });
    }

    /**
     * Stop a partition that is currently in-progress
     */
    @Override
    public void beforeWrite(@NonNull Chunk<? extends SerializedEobs> chunk) {
        stopReason().ifPresent(this::terminate);
    }

    private Optional<String> stopReason() {
        if (jobRepository.getJobStatusOfJob(jobUuid) == JobStatus.CANCELLED) {
            return Optional.of("was cancelled");
        }
        if (jobLease.isPauseRequested(jobUuid)) {
            return Optional.of("was asked to pause");
        }
        return Optional.empty();
    }

    private void terminate(String reason) {
        StepExecution stepExecution = currentStepExecution();
        if (stepExecution == null) {
            log.warn("job {} {}, but this chunk is unavailable. Will retry terminating the job next chunk.",
                    jobUuid, reason);
            return;
        }
        log.warn("job {} {}, the worker step will be terminated when this chunk finishes", jobUuid, reason);
        stepExecution.setTerminateOnly();
    }

    private static StepExecution currentStepExecution() {
        StepContext context = StepSynchronizationManager.getContext();
        return context == null ? null : context.getStepExecution();
    }
}
