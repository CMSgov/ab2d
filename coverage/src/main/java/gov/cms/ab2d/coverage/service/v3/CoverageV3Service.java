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
    // used for pause/resume to partition jobs
    CoveragePagingResult pageCoverageByPatientRange(String contract, long startPatientExclusive, long endPatientInclusive, Optional<Long> cursor, int pageSize);
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
    // Prototype pause/resume partitioning: highest row_number (true total row count, incl.
    // opt-outs and multi-MBI rows) and the patient_id boundaries that split it into partitions.
    long getMaxRowNumber(String contract);
    List<Long> getPartitionBoundaryPatientIds(String contract, int size);
    // NOTE: Assumes job has been kicked off and aggregated table exists -- this is a slow process and will be updated in AB2D-7272
    int getCoveragePeriodsInAggregatedTable(String contract);
    // used to find/delete tables from jobs not properly cleaned up
    void checkForAggregatedTablesToBeDeleted();
}
