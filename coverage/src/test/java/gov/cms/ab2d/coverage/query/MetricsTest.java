package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MetricsTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Test
	void test() throws Exception {

		val jobUuid = UUID.randomUUID().toString();
		val metrics = new Metrics(container.getDataSource());

		val metric1 = new Metrics.Metric(343000000L, 550L, 3, 52384, new long[1]);
		val metric2 = new Metrics.Metric(412000000L, 600L, 4, 65331, new long[1]);

		metric1.filterNs()[0] = 30000000L;
		metric2.filterNs()[0] = 40000000L;

		metrics.addMetric(jobUuid, new Metrics.Metric[]{metric1});
		metrics.addMetric(jobUuid, new Metrics.Metric[]{metric2});

		val preparedStatement = container.getDataSource().getConnection().prepareStatement("select * from v3.\"metrics_" + jobUuid + "\"");
		val result = preparedStatement.executeQuery();

		result.next();

		System.out.println();

		result.next();

		System.out.println();
	}

}
