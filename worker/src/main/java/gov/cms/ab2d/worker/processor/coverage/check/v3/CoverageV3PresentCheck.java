package gov.cms.ab2d.worker.processor.coverage.check.v3;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Count;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getAttestationTime;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getEndDateTime;

/**
 * Verifies enabled contracts have the expected number of coverage periods based on attestation date
 */
@Slf4j
public class CoverageV3PresentCheck extends CoverageV3CheckPredicate {

    public CoverageV3PresentCheck(CoverageV3Service coverageService, Map<String, List<YearMonthRecord>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(ContractDTO contract) {
        String contractNumber = contract.getContractNumber();
        if (!coverageCounts.containsKey(contractNumber)) {
            String issue = "[V3] " + contractNumber + " has no enrollment";
            log.warn(issue);
            issues.add(issue);
            return false;
        }

        List<String> noEnrollmentIssues = listCoveragePeriodsMissingEnrollment(contract, coverageCounts.get(contractNumber));
        issues.addAll(noEnrollmentIssues);

        return noEnrollmentIssues.isEmpty();
    }

    private List<String> listCoveragePeriodsMissingEnrollment(ContractDTO contract, List<YearMonthRecord> coverageCounts) {

        List<String> noEnrollment = new ArrayList<>();

        ZonedDateTime now = getEndDateTime().minusMonths(1);
        ZonedDateTime attestationTime = getAttestationTime(new ContractToContractCoverageMapping().map(contract));

        while (attestationTime.isBefore(now)) {
            int year = attestationTime.getYear();
            int month = attestationTime.getMonthValue();

            final boolean hasEnrollment = coverageCounts.stream()
                    .anyMatch(coverageCount -> {
	                    val matches = coverageCount.getYear() == year && coverageCount.getMonth() == month;
                        return matches;
                    });

            if (!hasEnrollment) {
                logIssue(contract, year, month, noEnrollment);
            }

            attestationTime = attestationTime.plusMonths(1);
        }

        return noEnrollment;
    }

    private void logIssue(ContractDTO contract, int year, int month, List<String> noEnrollment) {
        if (ignoreMissing(contract.getContractNumber(), year, month)) {
            return;
        }
        String issue = String.format("[V3] %s-%d-%d no enrollment found", contract.getContractNumber(), year, month);
        log.warn(issue);
        noEnrollment.add(issue);
    }

    /*
     * Block alert for S3147-2021-12 as it is expected to always fail
     * */
    private boolean ignoreMissing(@NotNull String contractNumber, int year, int month) {
        return contractNumber.equals("S3147") && year == 2021 && month == 12;
    }
}
