package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CoverageV3StagingServiceTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	CoverageV3StagingService syncService;

	@BeforeEach
	void setup() {
		syncService = new CoverageV3StagingService(container.getDataSource());
	}

	@Test
	void test() {

		int count = syncService.getCoveragePeriodCountForCoverageV3("Z1234");
		System.out.println(count);

		syncService.copyFromStagingTables("Z1234");

		int newCount = syncService.getCoveragePeriodCountForCoverageV3("Z1234");
		System.out.println(newCount);

		int newNewCount = syncService.getCoveragePeriodCountForCoverageV3Staging("Z1234");
		System.out.println(newNewCount);




	}


}
