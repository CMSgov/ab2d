package gov.cms.ab2d.api.controller;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.Optional;

import static gov.cms.ab2d.api.controller.JobCompletedResponse.CHECKSUM_STRING;
import static gov.cms.ab2d.api.controller.JobCompletedResponse.CONTENT_LENGTH_STRING;
import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_USER;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.FILE_ALREADY_DELETED;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

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
    private DoAll doAll;

    private String token;

    public static final String PATIENT_EXPORT_PATH = "/Patient/$export";

    private static final int MAX_JOBS_PER_USER = 3;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        doAll.delete();

        testUtil.turnMaintenanceModeOff();
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    private void createMaxJobs() throws Exception {
        for(int i = 0; i < MAX_JOBS_PER_USER; i++) {
            this.mockMvc.perform(
                    get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is(202));
        }
    }

    @Test
    public void testBasicPatientExport() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token));
                // .andDo(print());
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertEquals(1, apiRequestEvents.size());
        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(1, apiResponseEvents.size());
        assertEquals(HttpStatus.ACCEPTED.value(), responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        List<LoggableEvent> jobEvents = doAll.load(JobStatusChangeEvent.class);
        assertEquals(1, jobEvents.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals(JobStatus.SUBMITTED.name(), jobEvent.getNewStatus());
        assertNull(jobEvent.getOldStatus());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class)));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        assertEquals(job.getJobUuid(), responseEvent.getJobId());

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH);
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testBasicPatientExportWithHttps() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "https://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getRequestUrl(),
                "https://localhost" + API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH);
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testPatientExportDuplicateSubmission() throws Exception {
        createMaxJobs();

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(429))
                        .andExpect(header().string("Retry-After", "30"))
                        .andExpect(header().doesNotExist("X-Progress"));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(MAX_JOBS_PER_USER + 1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        assertEquals(MAX_JOBS_PER_USER + 1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(apiResponseEvents.size() - 1);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), responseEvent.getResponseCode());

        List<LoggableEvent> errorEvents = doAll.load(ErrorEvent.class);
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(ErrorEvent.ErrorType.TOO_MANY_STATUS_REQUESTS, errorEvent.getErrorType());

        List<LoggableEvent> jobEvents = doAll.load(JobStatusChangeEvent.class);
        assertEquals(MAX_JOBS_PER_USER, jobEvents.size());
        jobEvents.forEach(e -> assertEquals(JobStatus.SUBMITTED.name(), ((JobStatusChangeEvent) e).getNewStatus()));
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals(JobStatus.SUBMITTED.name(), jobEvent.getNewStatus());
        assertNull(jobEvent.getOldStatus());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(FileEvent.class)));
    }

    @Test
    public void testPatientExportDuplicateSubmissionWithCancelledStatus() throws Exception {
        createMaxJobs();

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testPatientExportDuplicateSubmissionWithInProgressStatus() throws Exception {
        createMaxJobs();

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testPatientExportDuplicateSubmissionWithDifferentUser() throws Exception {
        createMaxJobs();

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        User user = new User();
        Sponsor sponsor = dataSetup.createSponsor("Parent Spons", 4441, "Child Spons", 1114);
        user.setSponsor(sponsor);
        user.setEnabled(true);
        user.setUsername("test");
        user.setEmail("test@test.com");
        userRepository.saveAndFlush(user);

        for(Job job : jobs) {
            job.setUser(user);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testPatientExportWithParameters() throws Exception {
        final String typeParams =
                "?_type=ExplanationOfBenefit&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions =
                this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON));
                    // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + typeParams);
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testPatientExportWithInvalidType() throws Exception {
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefit";
        this.mockMvc.perform(get(API_PREFIX +  FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("_type must be ExplanationOfBenefit")));

        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertNull(requestEvent.getJobId());

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertNull(responseEvent.getJobId());
        assertEquals(400, responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class),
                doAll.load(JobStatusChangeEvent.class)));
    }

    @Test
    public void testPatientExportWithInvalidOutputFormat() throws Exception {
        final String typeParams = "?_outputFormat=Invalid";
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("An _outputFormat of Invalid is not " +
                                "valid")));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertNull(requestEvent.getJobId());

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertNull(responseEvent.getJobId());
        assertEquals(400, responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class),
                doAll.load(JobStatusChangeEvent.class)));
    }

    @Test
    public void testPatientExportWithZipOutputFormat() throws Exception {
        final String typeParams = "?_outputFormat=application/zip";
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
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
    public void testDeleteJob() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token));
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        this.mockMvc.perform(delete(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202))
                .andExpect(content().string(StringUtils.EMPTY));

        Job cancelledJob = jobRepository.findByJobUuid(job.getJobUuid());

        List<LoggableEvent> jobStatusChange = doAll.load(JobStatusChangeEvent.class);
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
    public void testDeleteNonExistentJob() throws Exception {
        this.mockMvc.perform(delete(API_PREFIX + FHIR_PREFIX + "/Job/NonExistentJob/$status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("No job with jobUuid NonExistentJob was " +
                                "found")));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        assertEquals("NonExistentJob", requestEvent.getJobId());

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        // Since the job does not exist, don't return it as the job id in the response event
        assertNull(responseEvent.getJobId());
        assertEquals(404, responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class),
                doAll.load(JobStatusChangeEvent.class)));

    }

    @Test
    public void testDeleteJobsInInvalidState() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        );
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        job.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(delete(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
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

        this.mockMvc.perform(delete(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
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

        this.mockMvc.perform(delete(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status")
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
    public void testGetStatusWhileSubmitted() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testGetStatusWhileInProgress() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testGetStatusWhileFinishedHttps() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
                .andExpect(header().string("Expires",
                        DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC)))
                .andExpect(jsonPath("$.transactionTime",
                        Is.is(new DateTimeType(job.getCreatedAt().toString()).toHumanDisplay())))
                .andExpect(jsonPath("$.request", Is.is(job.getRequestUrl())))
                .andExpect(jsonPath("$.requiresAccessToken", Is.is(true)))
                .andExpect(jsonPath("$.output[0].type", Is.is(EOB)))
                .andExpect(jsonPath("$.output[0].url",
                        Is.is("https://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/file.ndjson")))
                .andExpect(jsonPath("$.error[0].type", Is.is(OPERATION_OUTCOME)))
                .andExpect(jsonPath("$.error[0].url",
                        Is.is("https://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/error.ndjson")));
    }

    @Test
    public void testGetStatusWhileFinished() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
                .andExpect(header().string("Expires",
                        DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC)))
                .andExpect(jsonPath("$.transactionTime",
                        Is.is(new DateTimeType(job.getCreatedAt().toString()).toHumanDisplay())))
                .andExpect(jsonPath("$.request", Is.is(job.getRequestUrl())))
                .andExpect(jsonPath("$.requiresAccessToken", Is.is(true)))
                .andExpect(jsonPath("$.output[0].type", Is.is(EOB)))
                .andExpect(jsonPath("$.output[0].url",
                        Is.is("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
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
                        Is.is("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/error.ndjson")))
                .andExpect(jsonPath("$.error[0].extension[0].url",
                        Is.is(CHECKSUM_STRING)))
                .andExpect(jsonPath("$.error[0].extension[0].valueString",
                        Is.is("sha256:error")))
                .andExpect(jsonPath("$.error[0].extension[1].url",
                        Is.is(CONTENT_LENGTH_STRING)))
                .andExpect(jsonPath("$.error[0].extension[1].valueDecimal",
                        Is.is(6000)));

        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(2, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        ApiRequestEvent requestEvent2 = (ApiRequestEvent) apiRequestEvents.get(1);
        if (requestEvent.getUrl().contains("export")) {
            assertEquals(null, requestEvent.getJobId());
        } else {
            assertEquals(job.getJobUuid(), requestEvent.getJobId());
        }
        if (requestEvent2.getUrl().contains("export")) {
            assertEquals(null, requestEvent2.getJobId());
        } else {
            assertEquals(job.getJobUuid(), requestEvent2.getJobId());
        }

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
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
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class)));
    }

    @Test
    public void testGetStatusWhileFailed() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class)));
    }

    @Test
    public void testGetStatusWithJobNotFound() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/BadId/$status")
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
    public void testGetStatusWithSpaceUrl() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/ /$status")
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
    public void testGetStatusWithBadUrl() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/$status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404));
    }


    @Test
    public void testDownloadFile() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
        List<LoggableEvent> fileEvents = doAll.load(FileEvent.class);
        assertEquals(1, fileEvents.size());
        FileEvent fileEvent = (FileEvent) fileEvents.get(0);
        assertEquals(FileEvent.FileStatus.DELETE, fileEvent.getStatus());
        assertEquals(destinationStr + "/" + testFile, fileEvent.getFileName());
        assertNotNull(fileEvent.getFileHash());

        String downloadedFile = downloadFileCall.getResponse().getContentAsString();
        String testValue = JsonPath.read(downloadedFile, "$.test");
        assertEquals("value", testValue);
        String arrValue1 = JsonPath.read(downloadedFile, "$.array[0]");
        assertEquals("val1", arrValue1);
        String arrValue2 = JsonPath.read(downloadedFile, "$.array[1]");
        assertEquals("val2", arrValue2);

        Assert.assertTrue(!Files.exists(Paths.get(destinationStr + File.separator + testFile)));
    }

    @Test
    public void testDownloadMissingFileGenericError() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(apiRequestEvents.size() - 1);

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(apiResponseEvents.size() - 1);
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        List<LoggableEvent> errorEvents = doAll.load(ErrorEvent.class);
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(FILE_ALREADY_DELETED, errorEvent.getErrorType());
        assertEquals(errorEvent.getJobId(), requestEvent.getJobId());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(FileEvent.class)));
    }

    @Test
    public void testDownloadBadParameterFile() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
    public void testDownloadFileAlreadyDownloaded() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
    public void testDownloadFileExpired() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

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
    public void testBasicPatientExportWithContractWithHttps() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "https://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getRequestUrl(),
                "https://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export");
        assertEquals(job.getResourceTypes(), null);
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testBasicPatientExportWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export");
        assertEquals(job.getResourceTypes(), null);
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testPatientExportWithParametersWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams =
                "?_type=ExplanationOfBenefit&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions =
                this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON));
                // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams);
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testPatientExportWithInvalidTypeWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefit";
        this.mockMvc.perform(get(API_PREFIX +  FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
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
    public void testPatientExportWithInvalidOutputFormatWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams = "?_outputFormat=Invalid";
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
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

    @Test
    public void testPatientExportWithInvalidContract() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Group/" + "badContract" + "/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("Contract badContract was not found")));
    }

    private void createMaxJobsWithContract(Contract contract) throws Exception {
        for(int i = 0; i < MAX_JOBS_PER_USER; i++) {
            this.mockMvc.perform(
                    get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                            .andExpect(status().is(202));
        }
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmission() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmissionInProgress() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmissionCancelled() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmissionDifferentContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();

        for(int i = 0; i < MAX_JOBS_PER_USER - 1; i++) {
            this.mockMvc.perform(
                    get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is(202));
        }

        Sponsor sponsor = dataSetup.createSponsor("Parent Spons", 4441, "Child Spons", 1114);
        User user = userRepository.findByUsername(TEST_USER);
        user.setSponsor(sponsor);
        userRepository.saveAndFlush(user);
        Contract contractNew = dataSetup.setupContract(sponsor, "New Contract");

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contractNew.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmissionDifferentUser() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        User user = new User();
        Sponsor sponsor = dataSetup.createSponsor("Parent Spons", 4441, "Child Spons", 1114);
        user.setSponsor(sponsor);
        user.setEnabled(true);
        user.setUsername("test");
        user.setEmail("test@test.com");
        userRepository.saveAndFlush(user);

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for(Job job : jobs) {
            job.setUser(user);
            jobRepository.saveAndFlush(job);
        }

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testCapabilityStatement() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/metadata").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        assertEquals(body, new Gson().toJson(new CapabilityStatement()));
    }
}