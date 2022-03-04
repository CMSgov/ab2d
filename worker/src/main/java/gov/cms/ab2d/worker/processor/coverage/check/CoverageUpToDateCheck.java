package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.worker.model.ContractWorker;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Check that each coverage period has coverage data only from the most recent successful search and not from a
 * search in the past.
 */
@Slf4j
public class CoverageUpToDateCheck extends CoverageCheckPredicate {

    public CoverageUpToDateCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(ContractWorker contract) {

        // Do not refactor, want to report all issues for coverage not first one found for contract
        // No successful searches found for coverage service so some precondition has been violated anyway
        // Check that the last successful search is the one that we have coverage
        // information for and not an old one
        return coverageCounts.get(contract.getContractNumber()).stream().filter(count -> {
            Optional<CoverageSearchEvent> mostRecentSuccessfulSearch =
                    coverageService.findEventWithSuccessfulOffset(count.getCoveragePeriodId(), 1);

            // No successful searches found for coverage service so some precondition has been violated anyway
            if (mostRecentSuccessfulSearch.isEmpty()) {
                String issue = String.format("%s-%d-%d has no successful search in the history",
                        count.getContractNumber(), count.getYear(), count.getMonth());
                log.warn(issue);
                issues.add(issue);
                return true;
            }

            // Check that the last successful search is the one that we have coverage
            // information for and not an old one
            CoverageSearchEvent successfulEvent = mostRecentSuccessfulSearch.get();
            if (successfulEvent.getId() != count.getCoverageEventId()) {
                String issue = String.format("%s-%d-%d has coverage from an old coverage search not the most recent one",
                        count.getContractNumber(), count.getYear(), count.getMonth());
                log.warn(issue);
                issues.add(issue);
            }

            return successfulEvent.getId() != count.getCoverageEventId();
        }).count() == 0;
    }
}
