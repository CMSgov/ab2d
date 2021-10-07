package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageCount;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.service.CoverageService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Check to make sure that month to month enrollment changes are within acceptable bounds. If enrollment goes from
 * 1K to 1 million then there may be problem.
 */
@Slf4j
public class CoverageStableCheck extends CoverageCheckPredicate {

    private static final int CHANGE_THRESHOLD = 1000;
    private static final int CHANGE_PERCENT_THRESHOLD = 10;
    // Higher threshold for alerts for turn of the year
    private static final int CHANGE_PERCENT_THRESHOLD_NEW_YEAR = 33;

    public CoverageStableCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(Contract contract) {
        List<String> enrollmentChangeIssues = listCoveragePeriodsWithChangedEnrollment(coverageCounts.get(contract.getContractNumber()));
        issues.addAll(enrollmentChangeIssues);

        return enrollmentChangeIssues.isEmpty();
    }

    private List<String> listCoveragePeriodsWithChangedEnrollment(List<CoverageCount> coverageCounts) {

        List<String> coveragePeriodsChanged = new ArrayList<>();

        for (int idx = 1; idx < coverageCounts.size(); idx++) {
            CoverageCount previousMonth = coverageCounts.get(idx - 1);
            CoverageCount nextMonth = coverageCounts.get(idx);

            // Change could be anomaly for smaller contracts, ignore
            int change = Math.abs(previousMonth.getBeneficiaryCount() - nextMonth.getBeneficiaryCount());
            if (change < CHANGE_THRESHOLD) {
                continue;
            }

            int changePercentThreshold = getThreshold(previousMonth);
            double changePercent = 100.0 * change / previousMonth.getBeneficiaryCount();
            if (changePercentThreshold < changePercent) {
                String issue = String.format("%s enrollment changed %d%% between %d-%d and %d-%d",
                        previousMonth.getContractNumber(), (int) changePercent, previousMonth.getYear(),
                        previousMonth.getMonth(), nextMonth.getYear(), nextMonth.getMonth());

                log.warn(issue);
                coveragePeriodsChanged.add(issue);
            }
        }

        return coveragePeriodsChanged;
    }

    private static int getThreshold(CoverageCount previousMonth) {
        // At the turn of the year contracts change drastically so switch to a higher threshold for alerts
        if (previousMonth.getMonth() == 12) {
            return CHANGE_PERCENT_THRESHOLD_NEW_YEAR;
        }

        return CHANGE_PERCENT_THRESHOLD;
    }
}
