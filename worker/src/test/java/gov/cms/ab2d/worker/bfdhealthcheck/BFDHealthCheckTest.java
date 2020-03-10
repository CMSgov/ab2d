package gov.cms.ab2d.worker.bfdhealthcheck;

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

@SpringBootTest(classes = SpringBootApp.class, properties = "bfd.serverBaseUrl=http://localhost:8083/v1/fhir/")
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

    @Value("${bfd.health.check.consecutive.successes}")
    private int consecutiveSuccessesToBringUp;

    private static final int MOCK_SERVER_PORT = 8083;
    private static ClientAndServer mockServer;
    private static final String TEST_DIR = "test-data/";

    @BeforeAll
    public static void setupBFDClient() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
    }

    @Test
    public void testBfdGoingDown() throws IOException {
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("true", maintenanceProperties.getValue());

        // Cleanup
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);
        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }
    }

    @Test
    public void testBfdComingBackUp() throws IOException {
        // First take down, since BFD starts as up
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("true", maintenanceProperties.getValue());

        for (int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());
    }

    @Test
    public void testBfdGoingUpAndDown() throws IOException {
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveSuccessesToBringUp - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        bfdHealthCheck.checkBFDHealth();

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("true", maintenanceProperties.getValue());

        // Cleanup
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);
        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }
    }

    @Test
    public void testBfdGoingDownPastLimitAndComingBackUp() throws IOException {
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveFailuresToTakeDown + 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("true", maintenanceProperties.getValue());

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }
}

