package gov.cms.ab2d.coverage.service.v3;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

public interface CoverageV3SyncService {
	CoverageV3SyncResult copyFromStagingTablesToRecent(String contract, CoverageV3SyncSource source);

	CoverageV3SyncResult moveToHistorical(String contract, CoverageV3SyncSource source);

	List<String> getContractsWithActiveV3Jobs();

	boolean idrImporterInProgress();

	List<String> getContractsInRecentCoverageTable();

	List<String> getContractsInCoverageStagingTable();

	int deleteInactiveContractsFromHistorySummary();

	Supplier<LocalDate> CUT_OFF_DATE_FOR_INACTIVE_CONTRACT = () -> LocalDate.now().minusYears(2);
}
