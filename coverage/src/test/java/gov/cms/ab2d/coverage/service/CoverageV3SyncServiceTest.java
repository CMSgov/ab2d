package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.locks.Lock;

@Testcontainers
class CoverageV3SyncServiceTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	CoverageV3SyncService stagingService;

	@BeforeEach
	void setup() {
		val appContext = Mockito.mock(ApplicationContext.class);

		CoverageV3LockWrapperImpl wrapper = new CoverageV3LockWrapperImpl(appContext, container.getDataSource()) {

			@Override
			public Lock getCoverageLock(String contract) {
				val lock = Mockito.mock(Lock.class);
				return lock;
			}
		};

		val pdpClientService = Mockito.mock(PdpClientService.class);
		stagingService = new CoverageV3SyncService(container.getDataSource(), wrapper, pdpClientService);
	}

	@Test
	void test() {

		int count = stagingService.getCoveragePeriodCountForCoverageV3("Z1234");
		System.out.println(count);

//		stagingService.copyFromStagingTablesToRecent("Z1234");

//		int newCount = stagingService.getCoveragePeriodCountForCoverageV3("Z1234");
//		System.out.println(newCount);
//
//		int newNewCount = stagingService.getCoveragePeriodCountForCoverageV3Staging("Z1234");
//		System.out.println(newNewCount);

		stagingService.copyFromStagingTablesToRecent("Z0000");


		int rowsInsertedForZ0001 = stagingService.moveToHistoricalInternal("Z0000");
		System.out.println(rowsInsertedForZ0001);

		int rowsDeletedForZ0001 = stagingService.deleteMonthsOldCoverage("Z0000");
		System.out.println(rowsDeletedForZ0001);

		List<String> contractsWithActiveV3Jobs = stagingService.getContractsWithActiveV3Jobs();
		System.out.println(contractsWithActiveV3Jobs);


	}


}
