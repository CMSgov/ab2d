package gov.cms.ab2d.worker.processor.coverage.verifier;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageCount;
import gov.cms.ab2d.common.service.CoverageService;

import java.util.List;
import java.util.Map;

/**
 * Check that enrollment is from most recent successful search
 */
public class EnrollmentUpToDateCheck extends CoverageCheckPredicate {

    public EnrollmentUpToDateCheck(CoverageService coverageService, Map<String, List<CoverageCount>> coverageCounts, List<String> issues) {
        super(coverageService, coverageCounts, issues);
    }

    @Override
    public boolean test(Contract contract) {
        return false;
    }
}
