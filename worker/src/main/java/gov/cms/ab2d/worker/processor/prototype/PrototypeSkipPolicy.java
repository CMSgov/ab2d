package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.processor.prototype.lease.FenceLostException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Our policy is to skip a failing beneficiary unless it is failing for one of the following reasons, in
 * which case the job will be failed.
 *   - {@link FenceLostException}: this worker lost its lease
 *   - {@link InterruptedException}: a pause/shutdown in progress
 *   - {@link ThresholdExceededException}: too many skips
 */
public class PrototypeSkipPolicy implements SkipPolicy {

    /**
     * the skipCount argument is intentionally unused. We cap skips not by absolute count but
     * by percentage of total benes. This is handled with the threshold exception.
     */
    @Override
    public boolean shouldSkip(Throwable t, long skipCount) {
        return !isControlFlow(t) && !isControlFlow(t.getCause());
    }

    private static boolean isControlFlow(Throwable t) {
        return t instanceof FenceLostException
                || t instanceof InterruptedException
                || t instanceof ThresholdExceededException;
    }
}
