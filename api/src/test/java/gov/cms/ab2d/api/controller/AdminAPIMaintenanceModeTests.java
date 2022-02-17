package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.api.controller.BulkDataAccessAPIIntegrationTests.PATIENT_EXPORT_PATH;
import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIMaintenanceModeTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${efs.mount}")
    private String tmpJobLocation;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Autowired
    private DataSetup dataSetup;

    private static final String PROPERTIES_URL = "/properties";

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(Role.SPONSOR_ROLE, ADMIN_ROLE));
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        loggerEventRepository.delete();
    }

    @Test
    public void testSwitchMaintenanceModeOnAndOff() throws Exception {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO maintenanceModeDTO = new PropertiesDTO();
        maintenanceModeDTO.setKey(MAINTENANCE_MODE);
        maintenanceModeDTO.setValue("true");
        propertiesDTOs.add(maintenanceModeDTO);

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()));

        List<LoggableEvent> apiReqEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(2, apiReqEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiReqEvents.get(0);

        List<LoggableEvent> apiResEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, apiResEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResEvents.get(0);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), responseEvent.getResponseCode());

        List<LoggableEvent> reloadEvents = loggerEventRepository.load(ReloadEvent.class);
        assertEquals(1, reloadEvents.size());
        ReloadEvent reloadEvent = (ReloadEvent) reloadEvents.get(0);
        assertEquals(ReloadEvent.FileType.PROPERTIES, reloadEvent.getFileType());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)
        ));

        propertiesDTOs.clear();
        maintenanceModeDTO.setValue("false");
        propertiesDTOs.add(maintenanceModeDTO);

        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));

        jobRepository.findAll().forEach(job -> dataSetup.queueForCleanup(job));
    }

    @Test
    public void testJobsCanStillBeDownloadedWhileInMaintenanceMode() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202)).andReturn();
        String contentLocationUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);

        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO maintenanceModeDTO = new PropertiesDTO();
        maintenanceModeDTO.setKey(MAINTENANCE_MODE);
        maintenanceModeDTO.setValue("true");
        propertiesDTOs.add(maintenanceModeDTO);

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));

        String testFile = "test.ndjson";

        Job job = testUtil.createTestJobForDownload(testFile);
        dataSetup.queueForCleanup(job);

        String destinationStr = testUtil.createTestDownloadFile(tmpJobLocation, job, testFile);

        MvcResult mvcResultStatusCheck = this.mockMvc.perform(get(contentLocationUrl)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200)).andReturn();

        String downloadUrl = JsonPath.read(mvcResultStatusCheck.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Encoding", "gzip, deflate, br"))
                        .andExpect(status().is(200));

        assertFalse(Files.exists(Paths.get(destinationStr + File.separator + testFile)));

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