package gov.cms.ab2d.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.job.model.JobOutput;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static gov.cms.ab2d.api.controller.BulkDataAccessAPIIntegrationTests.PATIENT_EXPORT_PATH;
import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    SqsAsyncClient amazonSqs;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    JobClientMock jobClientMock;

    @Autowired
    SQSEventClient sqsEventClient;

    @Autowired
    private ApplicationContext context;

    private final PropertiesService propertiesService = new PropertyServiceStub();

    @Captor
    private ArgumentCaptor<LoggableEvent> captor;

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE, ADMIN_ROLE));
        ApiCommon apiCommon = context.getBean(ApiCommon.class);
        ReflectionTestUtils.setField(apiCommon, "propertiesService", propertiesService);
        propertiesService.createProperty(MAINTENANCE_MODE, "false");
        propertiesService.createProperty("ZipSupportOn", "false");
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        jobClientMock.cleanupAll();
    }

    @Test
    public void testSwitchMaintenanceModeOnAndOff() throws Exception {
        testUtil.turnMaintenanceModeOn();

        ApiCommon common = new ApiCommon(null, null, testUtil.getPropertiesService(),
                null, null);
        assertThrows(InMaintenanceModeException.class, () -> common.checkIfInMaintenanceMode());
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