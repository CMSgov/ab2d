package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.MockBfdServiceUtils;
import gov.cms.ab2d.worker.SpringBootApp;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static gov.cms.ab2d.common.util.Constants.MAINTENANCE_MODE;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class BFDHealthCheckTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private BFDHealthCheck bfdHealthCheck;

    @Autowired
    private PropertiesService propertiesService;

    @Value("${bfd.health.check.consecutive.failures}")
    private int consecutiveFailuresToTakeDown;

    private static int mockServerPort = 8083;
    private static ClientAndServer mockServer;
    private static final String TEST_DIR = "test-data/";

    @BeforeAll
    public static void setupBFDClient() {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);
    }

    @Test
    public void testBfdDown() throws IOException {
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", mockServerPort);

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("true", maintenanceProperties.getValue());
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }
}

