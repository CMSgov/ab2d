package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.SpringBootApp;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class BFDHealthCheckTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private BFDHealthCheck bfdHealthCheck;

    @Mock
    private BFDClient bfdClient;

    @Autowired
    private LogManager logManager;

    @Autowired
    private PropertiesAPIService propertiesApiService;

    @Value("${bfd.health.check.consecutive.failures}")
    private int consecutiveFailuresToTakeDown;

    @Value("${bfd.health.check.consecutive.successes}")
    private int consecutiveSuccessesToBringUp;

    private static final String TEST_DIR = "test-data/";

    private CapabilityStatement statement = new CapabilityStatement().setStatus(Enumerations.PublicationStatus.ACTIVE);

    @BeforeEach
    void setUp() {
        bfdHealthCheck = new BFDHealthCheck(logManager, propertiesApiService, bfdClient,
                consecutiveSuccessesToBringUp, consecutiveFailuresToTakeDown);
    }

    @Test
    void testBfdGoingDown() {
        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        String maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties);

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties);

        // test bfd coming back up
        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);
        for (int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
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

        String maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties);

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
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

        String maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties);

        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);

        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesApiService.getProperty(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties);
    }
}

