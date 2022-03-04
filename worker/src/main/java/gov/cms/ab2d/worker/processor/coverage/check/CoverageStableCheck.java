package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.worker.model.ContractWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Check to make sure that month to month enrollment changes are within acceptable bounds. If enrollment goes from
 * 1K to 1 million then there may be problem.
 */
@Slf4j
public class CoverageStableCheck extends CoverageCheckPredicate {

    private static final int CHANGE_THRESHOLD = 1000;
    private static final int CHANGE_PERCENT_THRESHOLD = 15;

    public CoverageStableCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(ContractWorker contract) {
        List<String> enrollmentChangeIssues = listCoveragePeriodsWithChangedEnrollment(coverageCounts.get(contract.getContractNumber()));
        issues.addAll(enrollmentChangeIssues);

        return enrollmentChangeIssues.isEmpty();
    }

    private List<String> listCoveragePeriodsWithChangedEnrollment(List<CoverageCount> coverageCounts) {

        List<String> coveragePeriodsChanged = new ArrayList<>();

        for (int idx = 1; idx < coverageCounts.size(); idx++) {
            CoverageCount previousMonth = coverageCounts.get(idx - 1);

            // Don't check December to January because changes can be 200% or more
            if (previousMonth.getMonth() == 12) {
                continue;
            }

            CoverageCount nextMonth = coverageCounts.get(idx);

            // Change could be anomaly for smaller contracts, ignore
            int change = Math.abs(previousMonth.getBeneficiaryCount() - nextMonth.getBeneficiaryCount());
            if (change < CHANGE_THRESHOLD) {
                continue;
            }

            double changePercent = 100.0 * change / previousMonth.getBeneficiaryCount();
            if (CHANGE_PERCENT_THRESHOLD < changePercent) {
                String issue = String.format("%s enrollment changed %d%% between %d-%d and %d-%d",
                        previousMonth.getContractNumber(), (int) changePercent, previousMonth.getYear(),
                        previousMonth.getMonth(), nextMonth.getYear(), nextMonth.getMonth());

                log.warn(issue);
                coveragePeriodsChanged.add(issue);
            }
        }

        return coveragePeriodsChanged;
    }
}
