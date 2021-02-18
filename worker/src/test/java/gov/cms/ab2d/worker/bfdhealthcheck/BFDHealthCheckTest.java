package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.MockBfdServiceUtils;
import gov.cms.ab2d.worker.SpringBootApp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static gov.cms.ab2d.common.util.Constants.MAINTENANCE_MODE;
import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;
import static gov.cms.ab2d.worker.bfdhealthcheck.BFDMockServerConfigurationUtil.MOCK_SERVER_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SpringBootApp.class)
@ContextConfiguration(initializers = BFDMockServerConfigurationUtil.PropertyOverrider.class)
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
        assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        // Cleanup
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);
        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }
    }

    @Test
    public void testBfdComingBackUp() throws IOException {
        // First take down, since BFD starts as up
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        for (int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());
    }

    @Test
    public void testBfdGoingUpAndDown() throws IOException {
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveSuccessesToBringUp - 1; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        bfdHealthCheck.checkBFDHealth(STU3);

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        // Cleanup
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);
        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }
    }

    @Test
    public void testBfdGoingDownPastLimitAndComingBackUp() throws IOException {
        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveFailuresToTakeDown + 1; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        MockBfdServiceUtils.reset(MOCK_SERVER_PORT);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", MOCK_SERVER_PORT);

        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth(STU3);
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }
}

