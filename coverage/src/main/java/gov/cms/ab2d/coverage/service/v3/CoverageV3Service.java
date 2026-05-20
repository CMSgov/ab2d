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
    CoverageV3SyncResult moveFromStagingToRecentCoverage(String contract, CoverageV3SyncSource source);
    CoverageV3SyncResult moveOldCoverageToHistoricalCoverage(String contract, CoverageV3SyncSource source);
    Map<String, List<CoverageV3Count>> getCoverageCount();
    boolean idrImportInProgress();
    // called before starting a v3 job
    void createAggregatedAttributionTable(String contract);
    // called when v3 job is completed, failed, or cancelled via API
    void deleteAggregatedAttributionTable(String contract);
    // NOTE: Assumes job has been kicked off and aggregated table exists
    int getDistinctPatientCount(String contract);
    // NOTE: Assumes job has been kicked off and aggregated table exists -- this is a slow process and will be updated in AB2D-7272
    int getCoveragePeriodsInAggregatedTable(String contract);
    // used to find/delete tables from jobs cancelled manually (not via the API)
    void checkForAggregatedTablesToBeDeleted();
}
