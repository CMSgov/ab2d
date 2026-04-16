package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Count;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;

import java.util.List;
import java.util.Map;

public interface CoverageV3Service {
    int countBeneficiariesByCoveragePeriod(CoverageV3Periods coveragePeriods, String contract);
    CoveragePagingResult pageCoverage(CoveragePagingRequest request);
    boolean moveFromStagingToRecentCoverage(String contract, boolean skipActiveJobCheck);
    boolean moveOldCoverageToHistoricalCoverage(String contract, boolean skipActiveJobCheck);
    Map<String, List<CoverageV3Count>> getCoverageCount();
}
