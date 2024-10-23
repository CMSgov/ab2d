package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.Constants.STATUS_ENDPOINT;
import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(AB2DSQSMockConfig.class)
public class MaintenanceModeAPITests {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    private PropertiesService propertiesService = new PropertyServiceStub();

    @BeforeEach
    public void setup() {
        MaintenanceModeAPI maintenanceModeAPI = context.getBean(MaintenanceModeAPI.class);
        ReflectionTestUtils.setField(maintenanceModeAPI, "propertiesService", propertiesService);
        propertiesService.createProperty(MAINTENANCE_MODE, "false");
    }

    @AfterEach
    void tearDown() {
        propertiesService.updateProperty(MAINTENANCE_MODE, "false");
    }

    @Test
    @Order(1)
    public void testMaintenanceModeOff() throws Exception {
        propertiesService.updateProperty(MAINTENANCE_MODE, "false");
        this.mockMvc.perform(get(STATUS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.maintenanceMode", Is.is("false")));
    }
}
