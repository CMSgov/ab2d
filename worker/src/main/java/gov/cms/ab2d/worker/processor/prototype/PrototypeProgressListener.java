package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.ProgressTracker;
import gov.cms.ab2d.worker.processor.SerializedEobs;
import gov.cms.ab2d.worker.service.JobChannelService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.HashMap;
import java.util.Map;

/**
 * Reports per-chunk progress to {@link ProgressTracker} and aborts the step when the failure
 * threshold is exceeded. A single instance is shared by all partition worker threads.
 *
 * The read counts come from the {@link StepExecution}, which this callback is not passed,
 * so it is read from the step context.
 */
@Slf4j
public class PrototypeProgressListener implements ItemWriteListener<SerializedEobs> {

    private final JobChannelService channel;
    private final JobProgressService progress;
    private final String jobUuid;
    // stepExecutionId -> last reported read count for the current run
    private final Map<Long, Long> reportedReads = new HashMap<>();

    public PrototypeProgressListener(JobChannelService channel, JobProgressService progress, String jobUuid) {
        this.channel = channel;
        this.progress = progress;
        this.jobUuid = jobUuid;
    }

    @Override
    public synchronized void afterWrite(@NonNull Chunk<? extends SerializedEobs> chunk) {
        StepExecution stepExecution = currentStepExecution();
        if (stepExecution == null) {
            log.warn("job {} afterWrite with no bound step context; skipping progress update", jobUuid);
            return;
        }

        long read = stepExecution.getReadCount();
        long readDelta = read - reportedReads.getOrDefault(stepExecution.getId(), 0L);
        reportedReads.put(stepExecution.getId(), read);

        // read count includes benes that were skipped, the subset that were skipped is counted
        // separately by the SkipCounter
        if (readDelta > 0) {
            channel.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_PROCESSED, readDelta);
        }
        if (!chunk.isEmpty()) {
            channel.sendUpdate(jobUuid, JobMeasure.PATIENTS_WITH_EOBS, chunk.size());
        }

        // Optimistic abort by checking failures against the failure threshold.
        // Checked again by the processor when the job finishes.
        ProgressTracker tracker = progress.getStatus(jobUuid);
        if (tracker != null && tracker.getTotalCount() > 0 && tracker.isErrorThresholdExceeded()) {
            log.warn("job {} tripped failure threshold mid-run ({} of {} benes failed) - aborting",
                    jobUuid, tracker.getPatientFailureCount(), tracker.getTotalCount());
            throw new ThresholdExceededException(jobUuid, tracker.getPatientFailureCount(),
                    tracker.getTotalCount(), tracker.getFailureThreshold());
        }
    }

    private static StepExecution currentStepExecution() {
        StepContext context = StepSynchronizationManager.getContext();
        return context == null ? null : context.getStepExecution();
    }
}
