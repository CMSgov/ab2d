package gov.cms.ab2d.api.controller.v3;

import com.okta.jwt.AccessTokenVerifier;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.TestUtil;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static gov.cms.ab2d.common.util.PropertyConstants.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3 jobs are submitted as pause/resume eligible when the feature is on, and a paused job says so in its status.
 */
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PauseResumeAPIV3Test {

    private static final String EXPORT_URL = "https://localhost:8443/api/v3/fhir/Patient/$export";
    private static final String STATUS_URL = "https://localhost:8443/api/v3/fhir/Job/%s/$status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobClientMock jobClientMock;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @DynamicPropertySource
    static void sqsProps(DynamicPropertyRegistry registry) {
        registry.add("AWS_SQS_URL", localstackContainer::getSqsEndpoint);
    }

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @MockitoBean
    SQSEventClient sqsEventClient;

    @MockitoBean
    AccessTokenVerifier mockAccessTokenVerifier;

    @Autowired
    private ApplicationContext context;

    private final PropertyServiceStub propertiesService = new PropertyServiceStub();

    private String token;

    @BeforeEach
    void setup() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
        testUtil.turnMaintenanceModeOff();
        ApiCommon apiCommon = context.getBean(ApiCommon.class);
        ReflectionTestUtils.setField(apiCommon, "propertiesService", propertiesService);
        propertiesService.createProperty(MAINTENANCE_MODE, "false");
        propertiesService.createProperty(V3_ON, "true");
        propertiesService.createProperty(V3_ALLOWLISTED_CONTRACTS, VALID_CONTRACT_NUMBER);
    }

    @AfterEach
    void cleanup() {
        dataSetup.cleanup();
        jobClientMock.cleanupAll();
    }

    @Test
    void v3JobIsPauseEligibleWhenFeatureIsOn() throws Exception {
        propertiesService.createProperty(PAUSE_RESUME_ENABLED, "true");

        startJob();

        assertTrue(jobClientMock.lookupJob(jobClientMock.pickAJob()).isPauseEligible());
    }

    @Test
    void v3JobIsNotPauseEligibleWhenFeatureIsOff() throws Exception {
        propertiesService.createProperty(PAUSE_RESUME_ENABLED, "false");

        startJob();

        assertFalse(jobClientMock.lookupJob(jobClientMock.pickAJob()).isPauseEligible());
    }

    @Test
    void pausedJobStatusSaysPaused() throws Exception {
        startJob();
        jobClientMock.setExpectedStatusAndProgress(JobStatus.SUBMITTED, 40);
        jobClientMock.setPaused(true);

        mockMvc.perform(get(String.format(STATUS_URL, jobClientMock.pickAJob()))
                        .contentType(FHIR_NDJSON_CONTENT_TYPE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Progress", "Paused, 40% complete"));
    }

    @Test
    void runningJobStatusIsUnchanged() throws Exception {
        startJob();
        jobClientMock.setExpectedStatusAndProgress(JobStatus.IN_PROGRESS, 40);

        mockMvc.perform(get(String.format(STATUS_URL, jobClientMock.pickAJob()))
                        .contentType(FHIR_NDJSON_CONTENT_TYPE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Progress", "40% complete"));
    }

    private void startJob() throws Exception {
        mockMvc.perform(get(EXPORT_URL).contentType(FHIR_NDJSON_CONTENT_TYPE)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isAccepted());
    }
}
