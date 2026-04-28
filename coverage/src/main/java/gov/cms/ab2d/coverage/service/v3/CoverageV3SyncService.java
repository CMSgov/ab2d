package gov.cms.ab2d.coverage.service.v3;

public interface CoverageV3SyncService {
	CoverageV3SyncResult copyFromStagingTablesToRecent(String contract, CoverageV3SyncSource source);
	CoverageV3SyncResult moveToHistorical(String contract, CoverageV3SyncSource source);
}
