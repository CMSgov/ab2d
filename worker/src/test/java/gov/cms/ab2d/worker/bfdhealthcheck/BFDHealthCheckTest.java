package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
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
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static gov.cms.ab2d.common.util.Constants.MAINTENANCE_MODE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private LogManager logManager;

    @Autowired
    private PropertiesService propertiesService;

    @Value("${bfd.health.check.consecutive.failures}")
    private int consecutiveFailuresToTakeDown;

    @Value("${bfd.health.check.consecutive.successes}")
    private int consecutiveSuccessesToBringUp;

    private static final String TEST_DIR = "test-data/";

    private CapabilityStatement statement = new CapabilityStatement().setStatus(Enumerations.PublicationStatus.ACTIVE);

    @BeforeEach
    public void setUp() {
        bfdHealthCheck = new BFDHealthCheck(logManager, propertiesService, bfdClient,
                consecutiveSuccessesToBringUp, consecutiveFailuresToTakeDown);
    }

    @Test
    public void testBfdGoingDown() {
        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        // test bfd coming back up
        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);
        for (int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());
    }

    @Test
    public void testBfdGoingUpAndDown() throws IOException {
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

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        // Cleanup
        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);
        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }
    }

    @Test
    public void testBfdGoingDownPastLimitAndComingBackUp() {
        when(bfdClient.capabilityStatement(eq(STU3))).thenThrow(new RuntimeException());

        for(int i = 0; i < consecutiveFailuresToTakeDown + 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("true", maintenanceProperties.getValue());

        when(bfdClient.capabilityStatement(eq(STU3))).thenReturn(statement);

        for(int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        assertEquals("false", maintenanceProperties.getValue());
    }
}

