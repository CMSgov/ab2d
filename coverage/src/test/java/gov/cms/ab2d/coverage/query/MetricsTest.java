package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MetricsTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Test
	void blah() {

		val jobUuid = UUID.randomUUID().toString();
		val metrics = new Metrics(container.getDataSource());
		metrics.createMetricsTable(jobUuid);


		metrics.insertMetrics(jobUuid, List.of(new Metrics.Metric(500L, 343L, 5L, 3, 52384)));
		metrics.insertMetrics(jobUuid, List.of(new Metrics.Metric(235L, 343L, 5L, 3, 52384)));

		System.out.println();
	}

}
