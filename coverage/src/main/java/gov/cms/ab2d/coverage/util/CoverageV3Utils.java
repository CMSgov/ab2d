package gov.cms.ab2d.coverage.util;

import gov.cms.ab2d.coverage.model.CoverageV3Periods;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.time.ZonedDateTime;
import java.util.ArrayList;


@UtilityClass
public class CoverageV3Utils {

    public static CoverageV3Periods enumerateCoveragePeriods(
            final ZonedDateTime startDateTime,
            final ZonedDateTime endDateTime
    ) {

        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException();
        }

        val threshold = endDateTime.minusMonths(4);
        val historicalCoverageTable = new ArrayList<YearMonthRecord>();
        val recentCoverageTable = new ArrayList<YearMonthRecord>();

        var currentYearMonth = startDateTime;
        while (currentYearMonth.isBefore(endDateTime)) {
            val periodToReport = new YearMonthRecord(
                currentYearMonth.getYear(),
                currentYearMonth.getMonthValue()
            );

            if (currentYearMonth.isBefore(threshold) || currentYearMonth.isEqual(threshold)) {
                historicalCoverageTable.add(periodToReport);
            }
            if (currentYearMonth.isAfter(threshold) || currentYearMonth.isEqual(threshold)) {
                recentCoverageTable.add(periodToReport);
            }

            currentYearMonth = currentYearMonth.plusMonths(1);
        }

        return CoverageV3Periods.builder()
            .historicalCoverage(historicalCoverageTable)
            .recentCoverage(recentCoverageTable)
            .build();
    }
}
