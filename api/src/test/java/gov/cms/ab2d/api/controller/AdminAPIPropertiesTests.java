package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminAPIPropertiesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    DataSetup dataSetup;

    @Autowired
    LoggerEventRepository loggerEventRepository;

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static final String PROPERTIES_URL = "/properties";

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(ADMIN_ROLE));
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        loggerEventRepository.delete();
    }

    @Test
    @Order(1)
    public void testRetrieveProperties() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>(){{
            put(PCP_CORE_POOL_SIZE, 10);
            put(PCP_MAX_POOL_SIZE, 150);
            put(PCP_SCALE_TO_MAX_TIME, 900);
            put(MAINTENANCE_MODE, "false");
            put(ZIP_SUPPORT_ON, "false");
            put(WORKER_ENGAGEMENT, "engaged");
            put(HPMS_INGESTION_ENGAGEMENT, "engaged");
            put(COVERAGE_SEARCH_DISCOVERY, "idle");
            put(COVERAGE_SEARCH_QUEUEING, "idle");
            put(COVERAGE_SEARCH_UPDATE_MONTHS, 1);
            put(COVERAGE_SEARCH_STUCK_HOURS, 24);
            put(COVERAGE_SEARCH_OVERRIDE, "false");
            put(MAX_DOWNLOADS, 30);
        }};

        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        List<PropertiesDTO> propertiesDTOs = mapper.readValue(result, new TypeReference<>() {} );

        assertEquals(13, propertiesDTOs.size());
        for(PropertiesDTO propertiesDTO : propertiesDTOs) {
            Object value = propertyMap.get(propertiesDTO.getKey());

            assertNotNull(value);
            assertEquals(value.toString(), propertiesDTO.getValue());
        }
    }

    @Test
    @Order(2)
    public void testUpdateProperties() throws Exception {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO pcpCorePoolSizeDTO = new PropertiesDTO();
        pcpCorePoolSizeDTO.setKey(PCP_CORE_POOL_SIZE);
        pcpCorePoolSizeDTO.setValue("15");
        propertiesDTOs.add(pcpCorePoolSizeDTO);

        PropertiesDTO pcpMaxPoolSizeDTO = new PropertiesDTO();
        pcpMaxPoolSizeDTO.setKey(PCP_MAX_POOL_SIZE);
        pcpMaxPoolSizeDTO.setValue("25");
        propertiesDTOs.add(pcpMaxPoolSizeDTO);

        PropertiesDTO pcpScaleToMaxTimeDTO = new PropertiesDTO();
        pcpScaleToMaxTimeDTO.setKey(PCP_SCALE_TO_MAX_TIME);
        pcpScaleToMaxTimeDTO.setValue("500");
        propertiesDTOs.add(pcpScaleToMaxTimeDTO);

        PropertiesDTO maintenanceModeDTO = new PropertiesDTO();
        maintenanceModeDTO.setKey(MAINTENANCE_MODE);
        maintenanceModeDTO.setValue("true");
        propertiesDTOs.add(maintenanceModeDTO);

        Map<String, Object> propertyMap = new HashMap<>(){{
            put(PCP_CORE_POOL_SIZE, 15);
            put(PCP_MAX_POOL_SIZE, 25);
            put(PCP_SCALE_TO_MAX_TIME, 500);
            put(MAINTENANCE_MODE, "true");
        }};

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        List<PropertiesDTO> propertiesDTOsRetrieved = mapper.readValue(result, new TypeReference<>() {} );

        assertEquals(4, propertiesDTOsRetrieved.size());
        for(PropertiesDTO propertiesDTO : propertiesDTOsRetrieved) {
            Object value = propertyMap.get(propertiesDTO.getKey());

            assertNotNull(value);
            assertEquals(value.toString(), propertiesDTO.getValue());
        }

        // Cleanup
        propertiesDTOs.clear();
        maintenanceModeDTO.setValue("false");
        propertiesDTOs.add(maintenanceModeDTO);

        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));
    }
}
