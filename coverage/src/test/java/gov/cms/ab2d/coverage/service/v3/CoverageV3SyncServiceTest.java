package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.locks.Lock;

@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CoverageV3SyncServiceTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	CoverageV3SyncServiceImpl stagingService;
	PropertiesService propertiesService;

	@Mock
	ApplicationContext appContext;

	@Mock
	Lock lock;

	@Mock
	CoverageV3LockWrapper lockWrapper;

	@BeforeEach
	void setup() {
		stagingService = new CoverageV3SyncServiceImpl(container.getDataSource(), lockWrapper, propertiesService);
	}


}
