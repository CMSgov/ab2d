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
import java.util.List;

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

    private static int mockServerPort = 8083;
    private static ClientAndServer mockServer;
    private static final String TEST_DIR = "test-data/";

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);
    }

    @Test
    public void testBfdGoingDown() throws IOException {
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", mockServerPort);

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("true", maintenanceProperties.getValue());
    }

    @Test
    public void testBfdComingBackUp() throws IOException {
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", mockServerPort);

        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(MAINTENANCE_MODE);
        propertiesDTO.setValue("true");
        List<PropertiesDTO> propertiesDTOs = propertiesService.updateProperties(List.of(propertiesDTO));

        PropertiesDTO maintenancePropertiesUpdated = propertiesDTOs.get(0);
        Assert.assertEquals("true", maintenancePropertiesUpdated.getValue());

        for (int i = 0; i < consecutiveSuccessesToBringUp; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());
    }

    @Test
    public void testBfdGoingUpAndDown() throws IOException {
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", mockServerPort);

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        MockBfdServiceUtils.reset(mockServerPort);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", mockServerPort);

        for(int i = 0; i < consecutiveSuccessesToBringUp - 1; i++) {
            bfdHealthCheck.checkBFDHealth();
        }

        MockBfdServiceUtils.reset(mockServerPort);
        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta-unknown-status.xml", mockServerPort);

        bfdHealthCheck.checkBFDHealth();

        Properties maintenanceProperties = propertiesService.getPropertiesByKey(MAINTENANCE_MODE);
        Assert.assertEquals("false", maintenanceProperties.getValue());

        for(int i = 0; i < consecutiveFailuresToTakeDown - 1; i++) {
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

