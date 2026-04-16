package gov.cms.ab2d.coverage.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CoverageV3ScheduledSync {

	private final CoverageV3SyncService syncService;

	public CoverageV3ScheduledSync(CoverageV3SyncService syncService) {
		this.syncService = syncService;
	}

	@Scheduled(cron= "0 0 * * * ?") // every hour
	public void copyFromStagingTablesToRecentForAllContracts() {
		val contracts = syncService.getContractsWithRecentCoverage();
		for (String contract : contracts) {
			try {
				syncService.copyFromStagingTablesToRecent(contract);
			} catch (Exception e) {
				log.info("Error calling copyFromStagingTablesToRecent for contract {}", contract);
			}
		}
	}

	@Scheduled(cron = "0 0 0 * * *") // every day at 12am
	public void moveToHistoricalForAllContracts() {
		val contracts = syncService.getContractsWithRecentCoverage();
		for (String contract : contracts) {
			try {
				syncService.moveToHistorical(contract);
			} catch (Exception e) {
				log.info("Error calling moveToHistorical for contract {}", contract);
			}
		}
	}


}
