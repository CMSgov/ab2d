package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;


import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getAttestationTime;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getEndDateTime;

/**
 * Check that a coverage period is not completely missing for a given contract
 */
@Slf4j
public class CoveragePeriodsPresentCheck extends CoverageCheckPredicate {

    public CoveragePeriodsPresentCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(ContractDTO contract) {
        ContractToContractCoverageMapping mapping = new ContractToContractCoverageMapping();
        List<String> missingPeriods = listMissingCoveragePeriods(this.coverageService, mapping.map(contract));
        this.issues.addAll(missingPeriods);

        return missingPeriods.isEmpty();
    }

    /**
     * Check that all coverage periods are necessary
     * @param contract contract to check enrollment for
     * @return list of issues found if any
     */
    private List<String> listMissingCoveragePeriods(CoverageService coverageService, ContractForCoverageDTO contract) {

        List<String> missingPeriods = new ArrayList<>();

        // Assume current time is EST since all AWS deployments are in EST
        ZonedDateTime now = getEndDateTime();
        ZonedDateTime attestationTime = getAttestationTime(contract);

        while (attestationTime.isBefore(now)) {
            try {
                coverageService.getCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());
            } catch (EntityNotFoundException enfe) {
                String issue = String.format("%s-%d-%d coverage period missing", contract.getContractNumber(),
                        attestationTime.getYear(), attestationTime.getMonthValue());

                log.warn(issue);
                missingPeriods.add(issue);
            }

            attestationTime = attestationTime.plusMonths(1);
        }

        return missingPeriods;
    }
}
