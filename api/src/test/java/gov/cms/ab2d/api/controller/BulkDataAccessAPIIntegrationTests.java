package gov.cms.ab2d.api.controller;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.apache.commons.io.IOUtils;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static gov.cms.ab2d.api.util.Constants.*;
import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
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

    private String token;

    private static final String PATIENT_EXPORT_PATH = "/Patient/$export";

    @BeforeEach
    public void setup() throws JwtVerificationException {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    @Test
    public void testBasicPatientExport() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH);
        Assert.assertEquals(job.getResourceTypes(), EOB);
        Assert.assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }

    @Test
    public void testPatientExportDuplicateSubmission() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(429))
                        .andExpect(header().string("Retry-After", "30"))
                        .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testPatientExportDuplicateSubmissionWithCancelledStatus() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testPatientExportDuplicateSubmissionWithInProgressStatus() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testPatientExportDuplicateSubmissionWithDifferentUser() throws Exception {
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        User user = new User();
        Sponsor sponsor = dataSetup.createSponsor("Parent Spons", 4441, "Child Spons", 1114);
        user.setSponsor(sponsor);
        user.setEnabled(true);
        user.setUsername("test");
        user.setEmail("test@test.com");
        userRepository.saveAndFlush(user);
        job.setUser(user);
        jobRepository.saveAndFlush(job);

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
                        .contentType(MediaType.APPLICATION_JSON)).andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH + typeParams);
        Assert.assertEquals(job.getResourceTypes(), EOB);
        Assert.assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
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
                        Is.is("InvalidUserInputException: _type must be ExplanationOfBenefit")));
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
                        Is.is("InvalidUserInputException: An _outputFormat of Invalid is not " +
                                "valid")));
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
        Assert.assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
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
                        Is.is("ResourceNotFoundException: No job with jobUuid NonExistentJob was " +
                                "found")));
        ;
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
                        Is.is("InvalidJobStateTransition: Job has a status of " + job.getStatus() +
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
                        Is.is("InvalidJobStateTransition: Job has a status of " + job.getStatus() +
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
                        Is.is("InvalidJobStateTransition: Job has a status of " + job.getStatus() +
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
        job.getJobOutputs().add(jobOutput);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setJob(job);
        errorJobOutput.setFilePath("error.ndjson");
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
                        Is.is(new DateTimeType(now.toString()).toHumanDisplay())))
                .andExpect(jsonPath("$.request", Is.is(job.getRequestUrl())))
                .andExpect(jsonPath("$.requiresAccessToken", Is.is(true)))
                .andExpect(jsonPath("$.output[0].type", Is.is(EOB)))
                .andExpect(jsonPath("$.output[0].url",
                        Is.is("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/file.ndjson")))
                .andExpect(jsonPath("$.error[0].type", Is.is(OPERATION_OUTCOME)))
                .andExpect(jsonPath("$.error[0].url",
                        Is.is("http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() +
                                "/file/error.ndjson")));
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
                        Is.is("JobProcessingException: Job failed while processing")));
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
                        Is.is("ResourceNotFoundException: No job with jobUuid BadId was found")));
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
                        Is.is("ResourceNotFoundException: No job with jobUuid   was found")));
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
        job.getJobOutputs().add(jobOutput);

        jobRepository.saveAndFlush(job);

        String testFile = "test.ndjson";
        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);
        InputStream testFileStream = this.getClass().getResourceAsStream("/" + testFile);
        String testFileStr = IOUtils.toString(testFileStream, "UTF-8");
        try (PrintWriter out = new PrintWriter(destinationStr + File.separator + testFile)) {
            out.println(testFileStr);
        }

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url");
        MvcResult downloadFileCall =
                this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(200))
                        .andExpect(content().contentType(NDJSON_FIRE_CONTENT_TYPE))
                        .andDo(MockMvcResultHandlers.print()).andReturn();
        String downloadedFile = downloadFileCall.getResponse().getContentAsString();
        String testValue = JsonPath.read(downloadedFile, "$.test");
        Assert.assertEquals("value", testValue);
        String arrValue1 = JsonPath.read(downloadedFile, "$.array[0]");
        Assert.assertEquals("val1", arrValue1);
        String arrValue2 = JsonPath.read(downloadedFile, "$.array[1]");
        Assert.assertEquals("val2", arrValue2);

        Assert.assertTrue(!Files.exists(Paths.get(destinationStr + File.separator + testFile)));
    }

    @Test
    public void testDownloadMissingFile() throws Exception {
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
        jobOutput.setFilePath("testmissing.ndjson");
        jobOutput.setError(false);
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
                        Is.is("JobOutputMissingException: The file is not present as it is either expired, been " +
                                "downloaded, or an error occurred. Please resubmit the job.")));
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

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setJob(job);
        jobOutput.setFilePath("test.ndjson");
        jobOutput.setError(false);
        job.getJobOutputs().add(jobOutput);

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
                        Is.is("ResourceNotFoundException: No Job Output with the file name test.ndjsonbadfilename exists in our records")));
    }

    @Test
    public void testBasicPatientExportWithContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export");
        Assert.assertEquals(job.getResourceTypes(), null);
        Assert.assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
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
                        .contentType(MediaType.APPLICATION_JSON)).andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams);
        Assert.assertEquals(job.getResourceTypes(), EOB);
        Assert.assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
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
                        Is.is("InvalidUserInputException: _type must be ExplanationOfBenefit")));
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
                        Is.is("InvalidUserInputException: An _outputFormat of Invalid is not " +
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
                        Is.is("ResourceNotFoundException: Contract number badContract was not found")));
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmission() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

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
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.saveAndFlush(job);

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
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testPatientExportWithContractDuplicateSubmissionDifferentContract() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

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
        this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        User user = new User();
        Sponsor sponsor = dataSetup.createSponsor("Parent Spons", 4441, "Child Spons", 1114);
        user.setSponsor(sponsor);
        user.setEnabled(true);
        user.setUsername("test");
        user.setEmail("test@test.com");
        userRepository.saveAndFlush(user);
        job.setUser(user);
        jobRepository.saveAndFlush(job);

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

        Assert.assertEquals(body, new Gson().toJson(new CapabilityStatement()));
    }
}