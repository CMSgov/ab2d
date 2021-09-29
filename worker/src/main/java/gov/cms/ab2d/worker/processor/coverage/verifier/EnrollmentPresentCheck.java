package gov.cms.ab2d.worker.processor.coverage.verifier;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageCount;
import gov.cms.ab2d.common.service.CoverageService;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getAttestationTime;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getEndDateTime;

@Slf4j
public class EnrollmentPresentCheck extends CoverageCheckPredicate {

    public EnrollmentPresentCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(Contract contract) {
        String contractNumber = contract.getContractNumber();
        if (!coverageCounts.containsKey(contractNumber)) {
            String issue = contractNumber + " has no enrollment";
            log.warn(issue);
            issues.add(issue);
            return true;
        }

        List<String> noEnrollmentIssues = listCoveragePeriodsMissingEnrollment(contract, coverageCounts.get(contractNumber));
        issues.addAll(noEnrollmentIssues);

        return !noEnrollmentIssues.isEmpty();
    }

    private List<String> listCoveragePeriodsMissingEnrollment(Contract contract, List<CoverageCount> coverageCounts) {

        List<String> noEnrollment = new ArrayList<>();

        ZonedDateTime now = getEndDateTime().minusMonths(1);
        ZonedDateTime attestationTime = getAttestationTime(contract);

        ListIterator<CoverageCount> countIterator = coverageCounts.listIterator();
        while (attestationTime.isBefore(now)) {
            int year = attestationTime.getYear();
            int month = attestationTime.getMonthValue();

            CoverageCount coverageCount = countIterator.next();
            if (year != coverageCount.getYear() || month != coverageCount.getMonth()) {
                countIterator.previous();

                String issue = String.format("%s-%d-%d no enrollment found", contract.getContractNumber(), year, month);
                log.warn(issue);
                noEnrollment.add(issue);
            }

            attestationTime = attestationTime.plusMonths(1);
        }

        return noEnrollment;
    }
}
