package gov.cms.ab2d.coverage.util;

import gov.cms.ab2d.coverage.model.CoverageV3Periods;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.time.ZonedDateTime;
import java.util.ArrayList;

@UtilityClass
public class CoverageV3Utils {


    public static CoverageV3Periods enumerateCoveragePeriods(final ZonedDateTime startDateTime, final ZonedDateTime endDateTime) {
        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException();
        }

        val threshold = endDateTime.minusMonths(4);
        val historicalCoverageTable = new ArrayList<YearMonthRecord>();
        val recentCoverageTable = new ArrayList<YearMonthRecord>();

        var timePeriod = startDateTime;
        while (timePeriod.isBefore(endDateTime)) {
            val periodToReport = new YearMonthRecord(
                    startDateTime.getYear(),
                    startDateTime.getMonthValue()
            );

            if (timePeriod.isBefore(threshold) || timePeriod.isEqual(threshold)) {
                historicalCoverageTable.add(periodToReport);
            }
            if (timePeriod.isAfter(threshold) || timePeriod.isEqual(threshold)) {
                recentCoverageTable.add(periodToReport);
            }

            timePeriod = timePeriod.plusMonths(1);
        }

        return CoverageV3Periods.builder()
            .historicalCoverage(historicalCoverageTable)
            .recentCoverage(recentCoverageTable)
            .build();

    }

}
