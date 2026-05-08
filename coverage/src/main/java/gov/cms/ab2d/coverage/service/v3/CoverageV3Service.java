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
    void createAggregatedAttributionTable(String contract);
    void deleteAggregatedAttributionTable(String contract);

    void deleteAggregatedTable(String tableName);

    // NOTE: Assumes job has been kicked off and aggregated table exists
    int getAggregatedTableRowCount(String contract);
    // NOTE: Assumes job has been kicked off and aggregated table exists
    int getCoveragePeriodsInAggregatedTable(String contract);
    void checkForAggregatedTablesToBeDeleted();
}
