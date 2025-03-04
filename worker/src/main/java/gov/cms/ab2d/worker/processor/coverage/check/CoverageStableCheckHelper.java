package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.coverage.model.CoverageCount;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

/**
 * Check to make sure that month to month enrollment changes are within acceptable bounds. If enrollment goes from
 * 1K to 1 million then there may be problem.
 */
@Slf4j
public class CoverageStableCheckHelper {
    private static final int CHANGE_THRESHOLD = 1000;

    //Moved the skip check conditions to a method to make sonar happy
    public static boolean skipCheck(CoverageCount previousMonth, CoverageCount nextMonth, int change) {

        // Don't check December to January because changes can be 200% or more
        LocalDate now = LocalDate.now();
        boolean skip = previousMonth.getMonth() == 12;

        // Ignores coverage checks from previous years
        if (nextMonth.getYear() < now.getYear() && previousMonth.getYear() < now.getYear()) {
            skip = true;
        }

        // January to February changes can also be significant.
        // Stop sending this notification once February ends.
        if (now.getMonthValue() > 2 && previousMonth.getMonth() == 1) {
            skip = true;
        }

        // Change could be anomaly for smaller contracts, ignore
        if (change < CHANGE_THRESHOLD) {
            skip = true;
        }
        return skip;
    }
}
