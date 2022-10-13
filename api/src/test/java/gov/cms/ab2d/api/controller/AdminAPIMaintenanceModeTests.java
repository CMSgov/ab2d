package gov.cms.ab2d.api.controller;

import com.amazonaws.services.sqs.AmazonSQS;
import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ContractSearchEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.events.ReloadEvent;
import gov.cms.ab2d.common.util.UtilMethods;
import gov.cms.ab2d.job.model.JobOutput;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.api.controller.BulkDataAccessAPIIntegrationTests.PATIENT_EXPORT_PATH;
import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class AdminAPIMaintenanceModeTests {

    @Autowired
    private MockMvc mockMvc;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    @Qualifier("mockAmazonSQS")
    AmazonSQS amazonSqs;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    JobClientMock jobClientMock;

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE, ADMIN_ROLE));
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        loggerEventRepository.delete();
        jobClientMock.cleanupAll();
    }

    @Test
    public void testSwitchMaintenanceModeOnAndOff() throws Exception {
        testUtil.turnMaintenanceModeOn();

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()));

        List<LoggableEvent> apiReqEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(1, apiReqEvents.size());

        List<LoggableEvent> apiResEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, apiResEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResEvents.get(0);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), responseEvent.getResponseCode());

        List<LoggableEvent> reloadEvents = loggerEventRepository.load(ReloadEvent.class);
        assertEquals(0, reloadEvents.size());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)
        ));

        testUtil.turnMaintenanceModeOff();

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testJobsCanStillBeDownloadedWhileInMaintenanceMode() throws Exception {
        testUtil.turnMaintenanceModeOff();

        MvcResult mvcResult = this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202)).andReturn();
        String contentLocationUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);

        String testFile = "test.ndjson";
        jobClientMock.addJobOutputForDownload(testUtil.createJobOutput(testFile));

        testUtil.turnMaintenanceModeOn();

        JobOutput jobOutput = testUtil.createJobOutput(testFile);
        jobClientMock.addJobOutputForDownload(jobOutput);
        jobClientMock.setResultsCreated(true);

        MvcResult mvcResultStatusCheck = this.mockMvc.perform(get(contentLocationUrl)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200)).andReturn();

        String downloadUrl = JsonPath.read(mvcResultStatusCheck.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Encoding", "gzip, deflate, br"))
                        .andExpect(status().is(200));

        // Cleanup
        testUtil.turnMaintenanceModeOff();
    }
}