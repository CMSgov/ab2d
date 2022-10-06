package gov.cms.ab2d.worker.properties;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.worker.config.AutoScalingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class PropertiesChangeDetectionTest {

    @Autowired
    private PropertiesChangeDetection propertiesChangeDetection;

    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @Autowired
    private AutoScalingService autoScalingService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void testChangeInProperties() {
        assertEquals(autoScalingService.getCorePoolSize(), 10);
        assertEquals(autoScalingService.getMaxPoolSize(), 150);
        assertEquals(autoScalingService.getScaleToMaxTime(), 900.0, 0);

        Properties propertiesCorePoolSize = propertiesRepository.findByKey(PCP_CORE_POOL_SIZE).get();
        propertiesCorePoolSize.setValue("8");
        propertiesRepository.save(propertiesCorePoolSize);

        Properties propertiesMaxPoolSize = propertiesRepository.findByKey(PCP_MAX_POOL_SIZE).get();
        propertiesMaxPoolSize.setValue("300");
        propertiesRepository.save(propertiesMaxPoolSize);

        Properties propertiesScaleToMaxTime = propertiesRepository.findByKey(PCP_SCALE_TO_MAX_TIME).get();
        propertiesScaleToMaxTime.setValue("1500");
        propertiesRepository.save(propertiesScaleToMaxTime);

        propertiesChangeDetection.detectChanges();

        assertEquals(autoScalingService.getCorePoolSize(), 8);
        assertEquals(autoScalingService.getMaxPoolSize(), 300);
        assertEquals(autoScalingService.getScaleToMaxTime(), 1500.0, 0);

        Object valuePCPCorePoolSize = configurableEnvironment.getPropertySources().get("application").getProperty(PCP_CORE_POOL_SIZE);
        assertEquals(valuePCPCorePoolSize, "8");

        Object valuePCPMaxPoolSize = configurableEnvironment.getPropertySources().get("application").getProperty(PCP_MAX_POOL_SIZE);
        assertEquals(valuePCPMaxPoolSize, "300");

        Object valuePCPScaleToMaxTime = configurableEnvironment.getPropertySources().get("application").getProperty(PCP_SCALE_TO_MAX_TIME);
        assertEquals(valuePCPScaleToMaxTime, "1500");

        // Cleanup
        propertiesCorePoolSize.setValue("10");
        propertiesRepository.save(propertiesCorePoolSize);
        propertiesMaxPoolSize.setValue("150");
        propertiesRepository.save(propertiesMaxPoolSize);
        propertiesScaleToMaxTime.setValue("900");
        propertiesRepository.save(propertiesScaleToMaxTime);
    }
}
