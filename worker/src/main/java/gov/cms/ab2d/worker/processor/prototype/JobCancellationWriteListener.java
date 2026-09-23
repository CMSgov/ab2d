package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.processor.SerializedEobs;
import gov.cms.ab2d.worker.processor.prototype.lease.JobLeaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

/**
 * Polls the AB2D job status once per chunk and, if the job has been cancelled, terminate
 * the worker step at the next chunk boundary
 */
@Slf4j
public class JobCancellationWriteListener implements ItemWriteListener<SerializedEobs> {

    private final JobRepository jobRepository;
    private final JobLeaseRepository jobLease;
    private final String jobUuid;

    public JobCancellationWriteListener(JobRepository jobRepository, JobLeaseRepository jobLease, String jobUuid) {
        this.jobRepository = jobRepository;
        this.jobLease = jobLease;
        this.jobUuid = jobUuid;
    }

    @Override
    public void beforeWrite(@NonNull Chunk<? extends SerializedEobs> chunk) {
        if (jobRepository.getJobStatusOfJob(jobUuid) == JobStatus.CANCELLED) {
            terminate("was cancelled");
            return;
        }
        if (jobLease.isPauseRequested(jobUuid)) {
            terminate("was asked to pause");
        }
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
