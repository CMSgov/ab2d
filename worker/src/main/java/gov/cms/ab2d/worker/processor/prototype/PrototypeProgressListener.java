package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.ProgressTracker;
import gov.cms.ab2d.worker.service.JobChannelService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Reports the progress of a job to {@link ProgressTracker} and checks to see if we've passed the
 * failure threshold.
 *
 * A single instance is shared by all partition worker threads.
 */
@Slf4j
public class PrototypeProgressListener implements ChunkListener {

    private final JobChannelService channel;
    private final JobProgressService progress;
    private final String jobUuid;
    // stepExecutionId -> last reported [read, write] for the current run
    private final Map<Long, long[]> reported = new HashMap<>();

    public PrototypeProgressListener(JobChannelService channel, JobProgressService progress, String jobUuid) {
        this.channel = channel;
        this.progress = progress;
        this.jobUuid = jobUuid;
    }

    @Override
    public synchronized void afterChunk(@NonNull ChunkContext context) {
        var stepExecution = context.getStepContext().getStepExecution();
        long read = stepExecution.getReadCount();
        long write = stepExecution.getWriteCount();

        long[] last = reported.getOrDefault(stepExecution.getId(), new long[2]);
        long readDelta = read - last[0];
        long writeDelta = write - last[1];
        reported.put(stepExecution.getId(), new long[]{read, write});

        // read count includes benes that were skipped, the subset that were skipped is counted
        // separately by the SkipCounter
        if (readDelta > 0) {
            channel.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_PROCESSED, readDelta);
        }
        if (writeDelta > 0) {
            channel.sendUpdate(jobUuid, JobMeasure.PATIENTS_WITH_EOBS, writeDelta);
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
}
