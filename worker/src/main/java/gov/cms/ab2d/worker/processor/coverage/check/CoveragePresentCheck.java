package gov.cms.ab2d.worker.processor.coverage.check;

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

/**
 * Check that each coverage period has some enrollment in the database. If no coverage found then something
 * went wrong saving or deleting coverage.
 *
 * Ignores the current month of the contract because sometimes the enrollment hasn't arrived during the current month
 * until late in the month.
 */
@Slf4j
public class CoveragePresentCheck extends CoverageCheckPredicate {

    public CoveragePresentCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
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

            // If nothing in the iterator
            if (!countIterator.hasNext()) {
                logIssue(contract, year, month, noEnrollment);
            // If something in iterator make sure it matches expected
            } else {
                CoverageCount coverageCount = countIterator.next();
                if (year != coverageCount.getYear() || month != coverageCount.getMonth()) {
                    countIterator.previous();
                    logIssue(contract, year, month, noEnrollment);
                }
            }

            attestationTime = attestationTime.plusMonths(1);
        }

        return noEnrollment;
    }

    private void logIssue(Contract contract, int year, int month, List<String> noEnrollment) {
        String issue = String.format("%s-%d-%d no enrollment found", contract.getContractNumber(), year, month);
        log.warn(issue);
        noEnrollment.add(issue);
    }
}
