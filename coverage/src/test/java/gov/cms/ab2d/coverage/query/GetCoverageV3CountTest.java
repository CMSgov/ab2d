package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GetCoverageV3CountTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	GetCoverageV3Count query;

	@BeforeEach
	void setup() {
		query = new GetCoverageV3Count(container.getDataSource());
	}

	@Test
	void test() {

		val result = query.coverageCounts();

		System.out.println(result);

	}

}
