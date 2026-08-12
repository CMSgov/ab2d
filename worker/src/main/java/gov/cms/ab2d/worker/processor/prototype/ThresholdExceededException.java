package gov.cms.ab2d.worker.processor.prototype;

/**
 * Thrown when the number of skipped benes exceeds the defined threshold, which is expressed
 * as a percentage of the total benes.
 */
public class ThresholdExceededException extends RuntimeException {

    public ThresholdExceededException(String jobUuid, int failures, int expected, int thresholdPercent) {
        super("job " + jobUuid + " exceeded failure threshold: " + failures + " of " + expected
                + " beneficiaries failed (over " + thresholdPercent + "%)");
    }
}
