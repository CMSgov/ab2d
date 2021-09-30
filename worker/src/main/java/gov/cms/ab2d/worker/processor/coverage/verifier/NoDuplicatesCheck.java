package gov.cms.ab2d.worker.processor.coverage.verifier;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageCount;
import gov.cms.ab2d.common.service.CoverageService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class NoDuplicatesCheck extends CoverageCheckPredicate {

    public NoDuplicatesCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(Contract contract) {

        Map<Integer, List<CoverageCount>> coverageByCoveragePeriod = coverageCounts.get(contract.getContractNumber())
                .stream().collect(groupingBy(CoverageCount::getCoveragePeriodId));


        coverageByCoveragePeriod.forEach((periodId, counts) -> {
            if (counts.size() > 1) {
                CoverageCount coverageCount = counts.get(0);
                String issue = String.format("%s-%d-%d has %d sets of enrollment when there should only be one",
                        coverageCount.getContractNumber(), coverageCount.getYear(), coverageCount.getMonth(),
                        counts.size());
                log.warn(issue);
                issues.add(issue);
            }
        });

        return coverageByCoveragePeriod.values().stream().anyMatch(counts -> counts.size() > 1);
    }
}
