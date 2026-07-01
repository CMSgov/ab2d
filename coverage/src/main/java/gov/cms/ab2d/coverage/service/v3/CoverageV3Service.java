package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Count;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CoverageV3Service {
    int countBeneficiariesByCoveragePeriod(CoverageV3Periods coveragePeriods, String contract);
    CoveragePagingResult pageCoverage(CoveragePagingRequest request);
    CoveragePagingResult pageCoverageByRowRange(String contract, long startRow, long endRow, Optional<Long> cursor, int pageSize);
    CoverageV3SyncResult moveFromStagingToRecentCoverage(String contract, CoverageV3SyncSource source);
    CoverageV3SyncResult moveOldCoverageToHistoricalCoverage(String contract, CoverageV3SyncSource source);
    Map<String, List<YearMonthRecord>> getCoveragePeriods(List<ContractDTO> contracts);
    boolean idrImportInProgress();
    // called before starting a v3 job
    void createAggregatedAttributionTable(String contract);
    // called when v3 job is completed, failed, or cancelled via API or manually in which case job UUID will be provided
    void deleteAggregatedTableForContract(String contract, Optional<String> jobUuid);
    // called by cron job to periodically clean up any old tables (in which case job UUID is not provided)
    void deleteAggregatedTable(String aggregatedTable);

    // NOTE: Assumes job has been kicked off and aggregated table exists
    int getDistinctPatientCount(String contract);
    // NOTE: Assumes job has been kicked off and aggregated table exists -- this is a slow process and will be updated in AB2D-7272
    int getCoveragePeriodsInAggregatedTable(String contract);
    // used to find/delete tables from jobs not properly cleaned up
    void checkForAggregatedTablesToBeDeleted();
}
