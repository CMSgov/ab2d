package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.health.PropertiesServiceAvailable;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.worker.SpringBootApp;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class BFDHealthCheckTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private BFDHealthCheck bfdHealthCheck;

    @Mock
    private BFDClient bfdClient;

    @Autowired
    private SQSEventClient logManager;

    @Autowired
    private PropertiesService propertiesService;

    @Value("${bfd.health.check.consecutive.failures}")
    private int consecutiveFailuresToTakeDown;

    @Value("${bfd.health.check.consecutive.successes}")
    private int consecutiveSuccessesToBringUp;

    private static final String TEST_DIR = "test-data/";

    private CapabilityStatement statement = new CapabilityStatement().setStatus(Enumerations.PublicationStatus.ACTIVE);

    @BeforeEach
    void setUp() {
        bfdHealthCheck = new BFDHealthCheck(logManager, propertiesService, bfdClient,
                consecutiveSuccessesToBringUp, consecutiveFailuresToTakeDown);
    }

    @Test
    void testBfdGoingDown() {
        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        String maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "false");
        assertEquals("false", maintenanceProperties);

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "true");
        assertEquals("true", maintenanceProperties);

        // test bfd coming back up
        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);
        for (int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "false");
        assertEquals("false", maintenanceProperties);
    }

    @Test
    void testBfdGoingUpAndDown() {
        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);

        for(int i = 0; i < consecutiveSuccessesToBringUp - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        bfdHealthCheck.checkBFDHealth();

        String maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "false");
        assertEquals("false", maintenanceProperties);

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "true");
        assertEquals("true", maintenanceProperties);

        // Cleanup
        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);
        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }
    }

    @Test
    void testBfdGoingDownPastLimitAndComingBackUp() {
        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        for(int i = 0; i < consecutiveFailuresToTakeDown + 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        String maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "true");
        assertEquals("true", maintenanceProperties);

        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);

        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getProperty(MAINTENANCE_MODE, "false");
        assertEquals("false", maintenanceProperties);
    }

    @Test
    void propertiesHealthy() {
        PropertiesServiceAvailable propertiesServiceAvailable = new PropertiesServiceAvailable(propertiesService);
        assertFalse(propertiesServiceAvailable.isAvailable(true));
        propertiesServiceAvailable = new PropertiesServiceAvailable(new PropertyServiceStub());
        assertTrue(propertiesServiceAvailable.isAvailable(true));
    }
}

