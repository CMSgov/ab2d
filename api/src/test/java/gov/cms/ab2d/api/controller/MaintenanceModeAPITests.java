package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.STATUS_ENDPOINT;
import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MaintenanceModeAPITests {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertiesAPIService propertiesApiService;

    @Autowired
    LoggerEventRepository loggerEventRepository;

    @AfterEach
    void tearDown() {
        propertiesApiService.updateProperty(MAINTENANCE_MODE, "false");
        loggerEventRepository.delete();
    }

    @Test
    @Order(1)
    public void testMaintenanceModeOff() throws Exception {
        propertiesApiService.updateProperty(MAINTENANCE_MODE, "false");
        this.mockMvc.perform(get(STATUS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.maintenanceMode", Is.is("false")));
    }

    @Test
    @Order(2)
    public void testMaintenanceModeOn() throws Exception {
        propertiesApiService.updateProperty(MAINTENANCE_MODE, "true");

        this.mockMvc.perform(get(STATUS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.maintenanceMode", Is.is("true")));
    }
}
