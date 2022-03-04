package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.worker.model.ContractWorker;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;


import static java.util.stream.Collectors.groupingBy;

/**
 * Check that each coverage period only has one set of coverage in the database from one search.
 */
@Slf4j
public class CoverageNoDuplicatesCheck extends CoverageCheckPredicate {

    public CoverageNoDuplicatesCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(ContractWorker contract) {

        // Not this checks responsibility if a contract is completely missing enrollment
        if (!coverageCounts.containsKey(contract.getContractNumber())) {
            return true;
        }

        Map<Integer, List<CoverageCount>> coverageByCoveragePeriod = coverageCounts.get(contract.getContractNumber())
                .stream().collect(groupingBy(CoverageCount::getCoveragePeriodId));

        return coverageByCoveragePeriod.entrySet().stream().filter(entry -> {
            List<CoverageCount> counts = entry.getValue();
            // If more than one record a warning
            if (counts.size() > 1) {
                CoverageCount coverageCount = counts.get(0);
                String issue = String.format("%s-%d-%d has %d sets of enrollment when there should only be one",
                        coverageCount.getContractNumber(), coverageCount.getYear(), coverageCount.getMonth(),
                        counts.size());

                log.warn(issue);
                issues.add(issue);
                return true;
            }

            return false;
        }).count() == 0;
    }
}
