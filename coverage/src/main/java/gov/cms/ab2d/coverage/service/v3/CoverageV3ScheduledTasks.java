package gov.cms.ab2d.coverage.service.v3;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncSource.CRON_JOB;

@Slf4j
@Service
public class CoverageV3ScheduledTasks {

	private final CoverageV3SyncService syncService;
	private final CoverageV3Service coverageV3Service;

	public CoverageV3ScheduledTasks(CoverageV3SyncService syncService, CoverageV3Service coverageV3Service) {
		this.syncService = syncService;
		this.coverageV3Service = coverageV3Service;
	}

	@Scheduled(cron= "0 0 * * * ?") // every hour
	public void copyFromStagingTablesToRecentForAllContracts() {
		log.info("Calling copyFromStagingTablesToRecentForAllContracts()");
		val contracts = syncService.getContractsInCoverageStagingTable();
		for (String contract : contracts) {
			try {
				val result = executeWithRetry(
					() -> syncService.copyFromStagingTablesToRecent(contract, CRON_JOB),
					List.of(SYNC_FAILED_FOR_CONTRACT, UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT),
					5,
					Duration.ofSeconds(15)
				);

				if (result == IDR_IMPORTER_IN_PROGRESS) {
					log.info("[V3] IDR import is in progress; Aborting copyFromStagingTablesToRecentForAllContracts()");
					return;
				} else if(result == JOB_IN_PROGRESS_FOR_CONTRACT) {
					log.info("[V3] Contract {} has a job in progress; Skipping moveToHistorical for contract {}", contract);
				} else if (result == SYNC_FAILED_FOR_CONTRACT) {
					log.error("[V3] Staging table sync failed for {}", contract);
				} else if (result == UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT) {
					log.error("[V3] Unable to acquire coverage lock for contract {}", contract);
				}

			} catch (Exception e) {
				log.error("Error calling copyFromStagingTablesToRecent for contract {}", contract);
			}
		}
	}

	// every day at 6:30am and 6:30pm -- staggered to not run during copyFromStagingTablesToRecentForAllContracts()
	@Scheduled(cron= "0 30 6,18 * * *")
	public void moveToHistoricalForAllContracts() {
		log.info("[V3] Calling moveToHistoricalForAllContracts()");
		val contracts = syncService.getContractsInRecentCoverageTable();
		for (String contract : contracts) {
			try {
				log.info("[V3] Calling moveToHistorical() for contract {}", contract);
				val result = executeWithRetry(
					() -> syncService.moveToHistorical(contract, CRON_JOB),
					List.of(SYNC_FAILED_FOR_CONTRACT, UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT),
					5,
					Duration.ofSeconds(15)
				);

				if (result == JOB_IN_PROGRESS_FOR_CONTRACT) {
					log.info("[V3] Contract {} has a job in progress; Skipping moveToHistorical", contract);
				} else if (result == SYNC_FAILED_FOR_CONTRACT) {
					log.error("[V3] Historical sync failed for {}", contract);
				} else if (result == UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT) {
					log.error("[V3] Unable to acquire coverage lock for contract {}", contract);
				}
			} catch (Exception e) {
				log.error("[V3] Error calling moveToHistorical for contract {}", contract);
			}
		}
	}

	@Scheduled(cron= "0 0 * * * ?") // every hour
	public void checkForAggregatedTablesToBeDeleted() {
		coverageV3Service.checkForAggregatedTablesToBeDeleted();
	}

	@Scheduled(cron = "0 0 3 * * *") // every day at 3am
	public void purgeInactiveContractsFromHistorySummary() {
		log.info("[V3] Purging history summary rows for contracts inactive > 2 years");
		int deleted = syncService.deleteInactiveContractsFromHistorySummary();
		log.info("[V3] Purged {} history summary rows for inactive contracts", deleted);
	}

	private CoverageV3SyncResult executeWithRetry(
			Supplier<CoverageV3SyncResult> supplier,
			List<CoverageV3SyncResult> retryableStates,
			int maxNumAttempts,
			Duration sleepTime) throws Exception {

		int attempts = 0;
		CoverageV3SyncResult result = null;
		do {
			attempts++;
			result = supplier.get();
			if (!retryableStates.contains(result)) {
				return result;
			}

			try {
				Thread.sleep(sleepTime.toMillis());
			} catch (InterruptedException e) {
				throw e;
			}
		}
		while (attempts <= maxNumAttempts);

		return result;
	}


}
