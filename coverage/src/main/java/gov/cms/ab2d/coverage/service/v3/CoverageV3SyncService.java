package gov.cms.ab2d.coverage.service.v3;

import java.util.List;

public interface CoverageV3SyncService {
	CoverageV3SyncResult copyFromStagingTablesToRecent(String contract, CoverageV3SyncSource source);
	CoverageV3SyncResult moveToHistorical(String contract, CoverageV3SyncSource source);

	List<String> getContractsWithActiveV3Jobs();

	boolean idrImporterInProgress();

	List<String> getContractsInRecentCoverageTable();

	List<String> getContractsInCoverageStagingTable();

}
