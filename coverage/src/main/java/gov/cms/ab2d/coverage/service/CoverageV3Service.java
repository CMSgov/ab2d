package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageV3Periods;

public interface CoverageV3Service {

    int countBeneficiariesByCoveragePeriod(CoverageV3Periods coveragePeriods, String contract);

    /**
     * Get a page of beneficiaries based on the provided request. If a cursor is provided use the cursor, otherwise
     * start with first page of beneficiaries.
     * @param request cursor pointing to starting point of next set of beneficiaries
     * @return result containing page of beneficiaries and request to get next page if more beneficiaries are present
     */
    CoveragePagingResult pageCoverage(CoveragePagingRequest request);


}
