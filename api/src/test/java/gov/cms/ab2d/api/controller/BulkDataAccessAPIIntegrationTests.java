package gov.cms.ab2d.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.v1.CapabilityStatementSTU3;
import gov.cms.ab2d.api.controller.v2.CapabilityStatementR4;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static gov.cms.ab2d.api.controller.JobCompletedResponse.CHECKSUM_STRING;
import static gov.cms.ab2d.api.controller.JobCompletedResponse.CONTENT_LENGTH_STRING;
import static gov.cms.ab2d.api.controller.common.ApiText.*;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.FILE_ALREADY_DELETED;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.http.HttpHeaders.EXPIRES;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
/* When checking in, comment out print statements. They are very helpful, but fill up the logs */
public class BulkDataAccessAPIIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Value("${efs.mount}")
    private String tmpJobLocation;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    private String token;

    public static final String PATIENT_EXPORT_PATH = "/Patient/$export";

    private static final int MAX_JOBS_PER_CLIENT = 3;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        testUtil.turnMaintenanceModeOff();
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    @AfterEach
    public void cleanup() {
        jobRepository.findAll().forEach(job -> dataSetup.queueForCleanup(job));  // catches implicitly generated jobs
        loggerEventRepository.delete();
        dataSetup.cleanup();
    }

    private void createMaxJobs() throws Exception {
        for(int i = 0; i < MAX_JOBS_PER_CLIENT; i++) {
            this.mockMvc.perform(
                    get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is(202));
        }
    }

    @Test
    void testBasicPatientExport() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token));
                // .andDo(print());
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertEquals(1, apiRequestEvents.size());
        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(1, apiResponseEvents.size());
        assertEquals(HttpStatus.ACCEPTED.value(), responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        List<LoggableEvent> jobEvents = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, jobEvents.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals(JobStatus.SUBMITTED.name(), jobEvent.getNewStatus());
        assertNull(jobEvent.getOldStatus());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class)));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        assertEquals(job.getJobUuid(), responseEvent.getJobId());

        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH, job.getRequestUrl());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), job.getOrganization());
    }

    @Test
    void testBasicPatientExportWithHttps() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH, job.getRequestUrl());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), job.getOrganization());
    }

    @Test
    void testPatientExportDuplicateSubmission() throws Exception {
        createMaxJobs();

        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(429))
                        .andExpect(header().string("Retry-After", "30"))
                        .andExpect(header().doesNotExist(X_PROG))
                        .andReturn();
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(MAX_JOBS_PER_CLIENT + 1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(MAX_JOBS_PER_CLIENT + 1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(apiResponseEvents.size() - 1);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), responseEvent.getResponseCode());

        List<LoggableEvent> errorEvents = loggerEventRepository.load(ErrorEvent.class);
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(ErrorEvent.ErrorType.TOO_MANY_STATUS_REQUESTS, errorEvent.getErrorType());

        List<LoggableEvent> jobEvents = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(MAX_JOBS_PER_CLIENT, jobEvents.size());
        jobEvents.forEach(e -> assertEquals(JobStatus.SUBMITTED.name(), ((JobStatusChangeEvent) e).getNewStatus()));
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals(JobStatus.SUBMITTED.name(), jobEvent.getNewStatus());
        assertNull(jobEvent.getOldStatus());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(FileEvent.class)));

        assertEquals(MAX_JOBS_PER_CLIENT, Objects.requireNonNull(mvcResult.getResponse().getHeader(CONTENT_LOCATION))
                .split(",").length);
    }

    @Test
    void testPatientExportDuplicateSubmissionWithCancelledStatus() throws Exception {
        createMaxJobs();

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    void testPatientExportDuplicateSubmissionWithInProgressStatus() throws Exception {
        createMaxJobs();

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist(X_PROG))
                .andExpect(header().exists(CONTENT_LOCATION));
    }

    @Test
    void testPatientExportDuplicateSubmissionWithDifferentClient() throws Exception {
        createMaxJobs();

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        PdpClient pdpClient = new PdpClient();
        Contract contract = dataSetup.setupContract("Test");
        pdpClient.setContract(contract);
        pdpClient.setEnabled(true);
        pdpClient.setClientId("test");
        pdpClient.setOrganization("test-org");
        pdpClientRepository.saveAndFlush(pdpClient);
        dataSetup.queueForCleanup(pdpClient);

        for(Job job : jobs) {
            job.setOrganization(pdpClient.getOrganization());
            jobRepository.saveAndFlush(job);
            dataSetup.queueForCleanup(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    void testPatientExportWithParameters() throws Exception {
        final String typeParams =
                "?_type=ExplanationOfBenefit&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions =
                this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON));
                    // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + typeParams, job.getRequestUrl());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), job.getOrganization());
    }

    @Test
    void testPatientExportWithInvalidType() throws Exception {
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefit";
        this.mockMvc.perform(get(API_PREFIX_V1 +  FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("_type must be ExplanationOfBenefit")));

        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertNull(requestEvent.getJobId());

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertNull(responseEvent.getJobId());
        assertEquals(400, responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)));
    }

    @Test
    void testPatientExportWithInvalidOutputFormat() throws Exception {
        final String typeParams = "?_outputFormat=Invalid";
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("An _outputFormat of Invalid is not " +
                                "valid")));
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertNull(requestEvent.getJobId());

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertNull(responseEvent.getJobId());
        assertEquals(400, responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)));
    }

    @Test
    void testPatientExportWithZipOutputFormat() throws Exception {
        final String typeParams = "?_outputFormat=application/zip";
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("An _outputFormat of application/zip is not valid")));
    }

    @Test
    void testDeleteJob() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token));
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        this.mockMvc.perform(delete(API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202))
                .andExpect(content().string(StringUtils.EMPTY));

        Job cancelledJob = jobRepository.findByJobUuid(job.getJobUuid());

        List<LoggableEvent> jobStatusChange = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(2, jobStatusChange.size());
        JobStatusChangeEvent event1 = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals("SUBMITTED", event1.getNewStatus());
        assertNull(event1.getOldStatus());
        JobStatusChangeEvent event2 = (JobStatusChangeEvent) jobStatusChange.get(1);
        assertEquals("SUBMITTED", event2.getOldStatus());
        assertEquals("CANCELLED", event2.getNewStatus());
        assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
    }

    @Test
    void testDeleteNonExistentJob() throws Exception {
        this.mockMvc.perform(delete(API_PREFIX_V1 + FHIR_PREFIX + "/Job/NonExistentJob/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("No job with jobUuid NonExistentJob was " +
                                "found")));
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertEquals("NonExistentJob", requestEvent.getJobId());

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        // Since the job does not exist, don't return it as the job id in the response event
        assertNull(responseEvent.getJobId());
        assertEquals(404, responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)));
    }

    @Test
    void testDeleteJobsInInvalidState() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        );
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        job.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(delete(API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("Job has a status of " + job.getStatus() +
                                ", so it cannot be cancelled")));

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(delete(API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("Job has a status of " + job.getStatus() +
                                ", so it cannot be cancelled")));

        job.setStatus(JobStatus.SUCCESSFUL);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(delete(API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("Job has a status of " + job.getStatus() +
                                ", so it cannot be cancelled")));
    }

    @Test
    void testGetStatusWhileSubmitted() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202))
                .andExpect(header().string("X-Progress", "0% complete"))
                .andExpect(header().string("Retry-After", "30"));

        // Immediate repeat of status check should produce 429.
        this.mockMvc.perform(get(statusUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"))
                .andExpect(header().doesNotExist(CONTENT_LOCATION));
    }

    @Test
    void testGetStatusWhileInProgress() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);


        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setProgress(30);
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202))
                .andExpect(header().string("X-Progress", "30% complete"))
                .andExpect(header().string("Retry-After", "30"));

        // Immediate repeat of status check should produce 429.
        this.mockMvc.perform(get(statusUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"))
                .andExpect(header().doesNotExist(CONTENT_LOCATION));
    }

    @Test
    void testGetStatusWhileFinishedHttps() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setJob(job);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setError(false);
        jobOutput.setChecksum("ABCD");
        jobOutput.setFileLength(10L);
        job.getJobOutputs().add(jobOutput);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setJob(job);
        errorJobOutput.setFilePath("error.ndjson");
        errorJobOutput.setError(true);
        errorJobOutput.setChecksum("1010F");
        errorJobOutput.setFileLength(20L);
        job.getJobOutputs().add(errorJobOutput);

        jobRepository.saveAndFlush(job);

        final ZonedDateTime jobExpiresUTC =
                ZonedDateTime.ofInstant(job.getExpiresAt().toInstant(), ZoneId.of("UTC"));
        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200))
                .andExpect(header().string(EXPIRES, DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC)))
                .andExpect(jsonPath("$.transactionTime",
                        Is.is(new org.hl7.fhir.dstu3.model.DateTimeType(job.getCreatedAt().toString()).toHumanDisplay())))
                .andExpect(jsonPath("$.request", Is.is(job.getRequestUrl())))
                .andExpect(jsonPath("$.requiresAccessToken", Is.is(true)))
                .andExpect(jsonPath("$.output[0].type", Is.is(EOB)))
                .andExpect(jsonPath("$.output[0].url",
                        Is.is("https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/file.ndjson")))
                .andExpect(jsonPath("$.error[0].type", Is.is(OPERATION_OUTCOME)))
                .andExpect(jsonPath("$.error[0].url",
                        Is.is("https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/error.ndjson")));
    }

    @Test
    void testGetStatusWhileFinished() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setJob(job);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setError(false);
        jobOutput.setFileLength(5000L);
        jobOutput.setChecksum("file");
        job.getJobOutputs().add(jobOutput);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setJob(job);
        errorJobOutput.setFilePath("error.ndjson");
        errorJobOutput.setFileLength(6000L);
        errorJobOutput.setChecksum("error");
        errorJobOutput.setError(true);
        job.getJobOutputs().add(errorJobOutput);

        jobRepository.saveAndFlush(job);

        final ZonedDateTime jobExpiresUTC =
                ZonedDateTime.ofInstant(job.getExpiresAt().toInstant(), ZoneId.of("UTC"));
        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200))
                .andExpect(header().string(EXPIRES, DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC)))
                .andExpect(jsonPath("$.transactionTime",
                        Is.is(new org.hl7.fhir.dstu3.model.DateTimeType(job.getCreatedAt().toString()).toHumanDisplay())))
                .andExpect(jsonPath("$.request", Is.is(job.getRequestUrl())))
                .andExpect(jsonPath("$.requiresAccessToken", Is.is(true)))
                .andExpect(jsonPath("$.output[0].type", Is.is(EOB)))
                .andExpect(jsonPath("$.output[0].url",
                        Is.is("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/file.ndjson")))
                .andExpect(jsonPath("$.output[0].extension[0].url",
                        Is.is(CHECKSUM_STRING)))
                .andExpect(jsonPath("$.output[0].extension[0].valueString",
                        Is.is("sha256:file")))
                .andExpect(jsonPath("$.output[0].extension[1].url",
                        Is.is(CONTENT_LENGTH_STRING)))
                .andExpect(jsonPath("$.output[0].extension[1].valueDecimal",
                        Is.is(5000)))
                .andExpect(jsonPath("$.error[0].type", Is.is(OPERATION_OUTCOME)))
                .andExpect(jsonPath("$.error[0].url",
                        Is.is("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/error.ndjson")))
                .andExpect(jsonPath("$.error[0].extension[0].url",
                        Is.is(CHECKSUM_STRING)))
                .andExpect(jsonPath("$.error[0].extension[0].valueString",
                        Is.is("sha256:error")))
                .andExpect(jsonPath("$.error[0].extension[1].url",
                        Is.is(CONTENT_LENGTH_STRING)))
                .andExpect(jsonPath("$.error[0].extension[1].valueDecimal",
                        Is.is(6000)));

        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(2, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        ApiRequestEvent requestEvent2 = (ApiRequestEvent) apiRequestEvents.get(1);
        if (requestEvent.getUrl().contains("export")) {
            assertNull(requestEvent.getJobId());
        } else {
            assertEquals(job.getJobUuid(), requestEvent.getJobId());
        }
        if (requestEvent2.getUrl().contains("export")) {
            assertNull(requestEvent2.getJobId());
        } else {
            assertEquals(job.getJobUuid(), requestEvent2.getJobId());
        }

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(2, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        ApiResponseEvent responseEvent2 = (ApiResponseEvent) apiResponseEvents.get(1);
        assertEquals(job.getJobUuid(), responseEvent.getJobId());
        assertEquals(job.getJobUuid(), responseEvent2.getJobId());
        assertEquals(200, responseEvent2.getResponseCode());

        assertTrue(requestEvent.getRequestId().equals(responseEvent.getRequestId()) ||
                requestEvent.getRequestId().equalsIgnoreCase(responseEvent2.getRequestId()));
        assertTrue(requestEvent2.getRequestId().equals(responseEvent.getRequestId()) ||
                requestEvent2.getRequestId().equalsIgnoreCase(responseEvent2.getRequestId()));
        assertEquals(requestEvent2.getRequestId(), responseEvent2.getRequestId());

        // Technically the job status change has 1 entry but should have more because
        // it went through the entire process, but because it is done manually here
        // events weren't created for it.

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class)));
    }

    @Test
    void testGetStatusWhileFailed() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("Job failed while processing")));
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class)));
    }

    @Test
    void testGetStatusWithJobNotFound() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/BadId/$status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("No job with jobUuid BadId was found")));
     }

    @Test
    void testGetStatusWithSpaceUrl() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/ /$status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("No job with jobUuid   was found")));
    }

    @Test
    void testGetStatusWithBadUrl() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/$status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404));
    }


    @Test
    void testDownloadFile() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        String testFile = "test.ndjson";

        Job job = testUtil.createTestJobForDownload(testFile);

        String destinationStr = testUtil.createTestDownloadFile(tmpJobLocation, job, testFile);

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url");
        MvcResult downloadFileCall =
                this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Encoding", "gzip, deflate, br"))
                        .andExpect(status().is(200))
                        .andExpect(content().contentType(NDJSON_FIRE_CONTENT_TYPE))
                        // .andDo(MockMvcResultHandlers.print())
                        .andReturn();
        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        assertEquals(1, fileEvents.size());
        FileEvent fileEvent = (FileEvent) fileEvents.get(0);
        assertEquals(FileEvent.FileStatus.DELETE, fileEvent.getStatus());
        assertEquals(destinationStr + File.separator + testFile, fileEvent.getFileName());
        assertNotNull(fileEvent.getFileHash());

        String downloadedFile = downloadFileCall.getResponse().getContentAsString();
        String testValue = JsonPath.read(downloadedFile, "$.test");
        assertEquals("value", testValue);
        String arrValue1 = JsonPath.read(downloadedFile, "$.array[0]");
        assertEquals("val1", arrValue1);
        String arrValue2 = JsonPath.read(downloadedFile, "$.array[1]");
        assertEquals("val2", arrValue2);

        assertFalse(Files.exists(Paths.get(destinationStr + File.separator + testFile)));
    }

    @Test
    void testDownloadMissingFileGenericError() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        testUtil.addJobOutput(job, "test.ndjson");

        jobRepository.saveAndFlush(job);

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("The file is not present as there was an error. Please resubmit the job.")));
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(apiRequestEvents.size() - 1);

        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(apiResponseEvents.size() - 1);
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        List<LoggableEvent> errorEvents = loggerEventRepository.load(ErrorEvent.class);
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(FILE_ALREADY_DELETED, errorEvent.getErrorType());
        assertEquals(errorEvent.getJobId(), requestEvent.getJobId());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(FileEvent.class)));
    }

    @Test
    void testDownloadBadParameterFile() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        testUtil.addJobOutput(job, "test.ndjson");

        jobRepository.saveAndFlush(job);
        dataSetup.queueForCleanup(job);

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url") + "badfilename";
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("No Job Output with the file name test.ndjsonbadfilename exists in our records")));
    }

    @Test
    void testDownloadFileAlreadyDownloaded() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setJob(job);
        jobOutput.setFilePath("test.ndjson");
        jobOutput.setError(false);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setDownloaded(true);
        job.getJobOutputs().add(jobOutput);

        jobRepository.saveAndFlush(job);

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("The file is not present as it has already been downloaded. Please resubmit the job.")));
    }

    @Test
    void testDownloadFileExpired() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().minusDays(2);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        testUtil.addJobOutput(job, "test.ndjson");

        jobRepository.saveAndFlush(job);

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("The file is not present as it has expired. Please resubmit the job.")));
    }

    @Test
    void testBasicPatientExportWithContractWithHttps() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";
        assertNotNull(statusUrl);

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export", job.getRequestUrl());
        assertNull(job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), job.getOrganization());
    }

    @Test
    void testBasicPatientExportWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export",
                job.getRequestUrl());
        assertNull(job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), job.getOrganization());
    }

    @Test
    void testPatientExportWithParametersWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams =
                "?_type=ExplanationOfBenefit&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions =
                this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams,
                job.getRequestUrl());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), job.getOrganization());
    }

    @Test
    void testPatientExportWithInvalidTypeWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefit";
        this.mockMvc.perform(get(API_PREFIX_V1 +  FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("_type must be ExplanationOfBenefit")));
    }

    @Test
    void testPatientExportWithInvalidOutputFormatWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams = "?_outputFormat=Invalid";
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("An _outputFormat of Invalid is not " +
                                "valid")));
    }

    private void createMaxJobsWithContract(Contract contract) throws Exception {
        for(int i = 0; i < MAX_JOBS_PER_CLIENT; i++) {
            this.mockMvc.perform(
                    get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                            .andExpect(status().is(202));
        }
    }

    @Test
    void testPatientExportWithContractDuplicateSubmission() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"))
                .andExpect(header().exists(CONTENT_LOCATION));
    }

    @Test
    void testPatientExportWithContractDuplicateSubmissionInProgress() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"))
                .andExpect(header().exists(CONTENT_LOCATION));

    }

    @Test
    void testPatientExportWithContractDuplicateSubmissionCancelled() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    void testPatientExportWithContractDuplicateSubmissionDifferentContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();

        for(int i = 0; i < MAX_JOBS_PER_CLIENT - 1; i++) {
            this.mockMvc.perform(
                    get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is(202));
        }

        Contract contract1 = dataSetup.setupContract("Test1");

        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        assertNotNull(pdpClient);
        pdpClient.setContract(contract1);
        pdpClientRepository.saveAndFlush(pdpClient);
        Contract contractNew = dataSetup.setupContract("New Contract");

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contractNew.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    void testPatientExportWithContractDuplicateSubmissionDifferentClient() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        PdpClient pdpClient = new PdpClient();
        pdpClient.setContract(contract);
        pdpClient.setEnabled(true);
        pdpClient.setClientId("test");
        pdpClient.setOrganization("test-org");
        pdpClientRepository.saveAndFlush(pdpClient);
        dataSetup.queueForCleanup(pdpClient);

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setOrganization(pdpClient.getOrganization());
            jobRepository.saveAndFlush(job);
            dataSetup.queueForCleanup(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    void testCapabilityStatementSTU3() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get("https://localhost:8443/" + API_PREFIX_V1 + FHIR_PREFIX + "/metadata").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        assertEquals(body, STU3.getJsonParser().encodeResourceToString(
                CapabilityStatementSTU3.populateCS("https://localhost:8443" + API_PREFIX_V1 + FHIR_PREFIX)));
    }

    @Test
    void testCapabilityStatementR4() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get("https://localhost:8443/" + API_PREFIX_V2 + FHIR_PREFIX + "/metadata").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        assertEquals(body, R4.getJsonParser().encodeResourceToString(
                CapabilityStatementR4.populateCS("https://localhost:8443" + API_PREFIX_V2 + FHIR_PREFIX)));
    }

    @Test
    void tlsTest() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get("https://localhost:8443" + API_PREFIX_V1 + FHIR_PREFIX + "/metadata").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        assertEquals(body, STU3.getJsonParser().encodeResourceToString(
                CapabilityStatementSTU3.populateCS("https://localhost:8443" + API_PREFIX_V1 + FHIR_PREFIX)));
    }
}