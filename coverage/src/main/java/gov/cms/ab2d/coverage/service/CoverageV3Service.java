package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageV3Periods;

public interface CoverageV3Service {
    int countBeneficiariesByCoveragePeriod(CoverageV3Periods coveragePeriods, String contract);
    CoveragePagingResult pageCoverage(CoveragePagingRequest request);
}
