package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CoverageV3ServiceImplTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Mock
	CoverageV3SyncServiceImpl syncService;
	@Mock
	PropertiesService propertiesService;

	CoverageV3ServiceImpl service = new CoverageV3ServiceImpl(container.getDataSource(), propertiesService, syncService);


	@BeforeEach
	void setup() {
		service = new CoverageV3ServiceImpl(container.getDataSource(), propertiesService, syncService);
	}

	@Test
	void test() {
		assertTrue(
			service.shouldDeleteAggregatedTable(
				"coverage_v3_aggregated_s5601",
				List.of("S4802")
			)
		);

		assertFalse(
			service.shouldDeleteAggregatedTable(
					"coverage_v3_aggregated_s5601",
					List.of("s5601")
			)
		);

		assertFalse(
			service.shouldDeleteAggregatedTable(
					"coverage_v3_aggregated_s5601",
					List.of("S5601")
			)
		);
	}


}
