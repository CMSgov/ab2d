package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BfdMetricsUtilityTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Test
	void test() throws Exception {

		val jobUuid = UUID.randomUUID().toString();
		val metrics = new BfdMetricsUtility(container.getDataSource());

		val metric1 = new BfdMetricsUtility.BfdRequestMetric(1_000_001, 1_001, 1, 3501, 501);
		val metric2 = new BfdMetricsUtility.BfdRequestMetric(1_000_002, 1_001, 2, 3502, 502);

		metrics.addMetric(jobUuid, metric1);
		metrics.addMetric(jobUuid, metric2);

		val preparedStatement = container.getDataSource().getConnection().prepareStatement("select * from v3.\"metrics_" + jobUuid + "\"");
		val result = preparedStatement.executeQuery();

		result.next();

		System.out.println();

		result.next();

		System.out.println();
	}

}
