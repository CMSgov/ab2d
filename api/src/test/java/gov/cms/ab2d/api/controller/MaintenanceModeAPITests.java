package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class MaintenanceModeAPITests {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertiesService propertiesService;

    @Test
    public void testMaintenanceModeOff() throws Exception {
        this.mockMvc.perform(get(MAINTENANCE_PREFIX + "/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.maintenanceMode", Is.is("false")));
    }

    @Test
    public void testMaintenanceModeOn() throws Exception {
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(MAINTENANCE_MODE);
        propertiesDTO.setValue("true");
        propertiesService.updateProperties(List.of(propertiesDTO));

        this.mockMvc.perform(get(MAINTENANCE_PREFIX + "/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.maintenanceMode", Is.is("true")));
    }
}
