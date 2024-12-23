package gov.cms.ab2d.api.controller;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.controller.v1.CapabilityStatementSTU3;
import gov.cms.ab2d.api.controller.v2.CapabilityStatementR4;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.eventclient.clients.SQSConfig;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.job.model.JobOutput;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hamcrest.collection.IsIn;
import org.hamcrest.core.Is;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.api.controller.common.ApiText.X_PROG;
import static gov.cms.ab2d.api.remote.JobClientMock.EXPIRES_IN_DAYS;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.MAX_DOWNLOADS;
import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.job.model.JobStatus.SUBMITTED;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.http.HttpHeaders.EXPIRES;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
        /* When checking in, comment out print statements. They are very helpful, but fill up the logs */
class BulkDataAccessAPIIntegrationTests {

    private static final String FILE_NOT_PRESENT_ERROR = "The file is not present as there was an error. Please resubmit the job.";
    private static final String MAX_DOWNLOAD_EXCEDED_ERROR = "The file has reached the maximum number of downloads. Please resubmit the job.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JobClientMock jobClientMock;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private ContractServiceStub contractServiceStub;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    AmazonSQS amazonSQS;

    @Autowired
    SQSEventClient sqsEventClient;

    @Autowired
    private ApplicationContext context;

    private final PropertiesService propertiesService = new PropertyServiceStub();

    private String token;

    public static final String PATIENT_EXPORT_PATH = "/Patient/$export";

    private static final int MAX_JOBS_PER_CLIENT = 3;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
        testUtil.turnMaintenanceModeOff();
        ApiCommon apiCommon = context.getBean(ApiCommon.class);
        ReflectionTestUtils.setField(apiCommon, "propertiesService", propertiesService);
        propertiesService.createProperty(MAINTENANCE_MODE, "false");
    }

    @AfterEach
    public void cleanup() {
        jobClientMock.cleanupAll();
        dataSetup.cleanup();
        amazonSQS.purgeQueue(new PurgeQueueRequest(System.getProperty("sqs.queue-name")));
    }

    private void createMaxJobs() throws Exception {
        for (int i = 0; i < MAX_JOBS_PER_CLIENT; i++) {
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

        ApiRequestEvent requestEvent = (ApiRequestEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().get(0).getBody(), GeneralSQSMessage.class).getLoggableEvent();
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().get(0).getBody(), GeneralSQSMessage.class).getLoggableEvent();
        ApiResponseEvent responseEvent = (ApiResponseEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().get(0).getBody(), GeneralSQSMessage.class).getLoggableEvent();


        assertEquals(HttpStatus.ACCEPTED.value(), responseEvent.getResponseCode());

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        assertEquals(SUBMITTED.name(), jobEvent.getNewStatus());
        assertNull(jobEvent.getOldStatus());

        String jobUuid = jobClientMock.pickAJob();
        assertEquals(jobUuid, responseEvent.getJobId());

        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + jobUuid + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobUuid);
        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH, startJobDTO.getUrl());
        assertEquals(EOB, startJobDTO.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), startJobDTO.getOrganization());

        assertEquals(0, amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().size());
    }

    @Test
    void testBasicPatientExportWithHttps() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));

        String jobUuid = jobClientMock.pickAJob();
        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobUuid);
        String statusUrl =
                "https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + jobUuid + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals("https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH, startJobDTO.getUrl());
        assertEquals(EOB, startJobDTO.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), startJobDTO.getOrganization());
    }

    @Test
    void testPatientExportDuplicateSubmission() throws Exception, JsonProcessingException {
        createMaxJobs();

        MvcResult mvcResult = this.mockMvc.perform(
                        get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist(X_PROG))
                .andReturn();
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(System.getProperty("sqs.queue-name"));
        receiveMessageRequest.setMaxNumberOfMessages(15);

        List<Message> events = amazonSQS.receiveMessage(receiveMessageRequest).getMessages();

        assertEquals(12, events.size());

        List<ApiRequestEvent> apiRequestEvents = events.stream().filter(e -> e.toString().contains("ApiRequestEvent")).map(e -> (ApiRequestEvent)getRequestEvent(e)).toList();
        List<ApiResponseEvent> apiResponseEvents = events.stream().filter(e -> e.toString().contains("ApiResponseEvent")).map(e -> (ApiResponseEvent)getRequestEvent(e)).toList();
        List<ErrorEvent> errorEvents = events.stream().filter(e -> e.toString().contains("ErrorEvent")).map(e -> (ErrorEvent)getRequestEvent(e)).toList();
        List<JobStatusChangeEvent> jobEvents = events.stream().filter(e -> e.toString().contains("JobStatusChangeEvent")).map(e -> (JobStatusChangeEvent)getRequestEvent(e)).toList();

        assertEquals(MAX_JOBS_PER_CLIENT + 1, apiRequestEvents.size());
        ApiResponseEvent responseEvent = apiResponseEvents.get(apiResponseEvents.size() - 1);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), responseEvent.getResponseCode());

        ErrorEvent errorEvent = errorEvents.get(0);
        assertEquals(ErrorEvent.ErrorType.TOO_MANY_STATUS_REQUESTS, errorEvent.getErrorType());

        assertEquals(MAX_JOBS_PER_CLIENT, jobEvents.size());
        jobEvents.forEach(e -> assertEquals(SUBMITTED.name(), e.getNewStatus()));
        JobStatusChangeEvent jobEvent = jobEvents.get(0);
        assertEquals(SUBMITTED.name(), jobEvent.getNewStatus());
        assertNull(jobEvent.getOldStatus());
    }

    @Nullable
    private static LoggableEvent getRequestEvent(Message e) {
        try {
            return SQSConfig.objectMapper().readValue(e.getBody(), GeneralSQSMessage.class).getLoggableEvent();
        } catch (JsonProcessingException ex) {
            fail("Should not get here");
            return null;
        }
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

        jobClientMock.addJobOutputForDownload(testUtil.createJobOutput("test.ndjson"));

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

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath("test.ndjson");
        jobOutput.setError(false);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setDownloaded(MAX_DOWNLOADS - 1);
        jobClientMock.setResultsCreated(true);
        jobClientMock.addJobOutputForDownload(jobOutput);

        MvcResult mvcResultStatusCall =
                this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                        .andReturn();
        String downloadUrl = JsonPath.read(mvcResultStatusCall.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Encoding", "gzip, deflate, br"))
                .andExpect(status().is(200))
                .andExpect(content().contentType(FHIR_NDJSON_CONTENT_TYPE))
                .andReturn();
    }


    @Test
    void testDownloadCountExceed() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                        get(API_PREFIX_V1 + FHIR_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        String statusUrl = mvcResult.getResponse().getHeader(CONTENT_LOCATION);
        assertNotNull(statusUrl);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath("test.ndjson");
        jobOutput.setError(false);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setDownloaded(MAX_DOWNLOADS);
        jobClientMock.addJobOutputForDownload(jobOutput);

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
                        Is.is("The file has reached the maximum number of downloads. Please resubmit the job.")));
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

        OffsetDateTime expireDate = OffsetDateTime.now().minusDays(2);
        jobClientMock.setExpiresAt(expireDate);
        JobOutput jobOutput = testUtil.createJobOutput("test.ndjson");
        jobClientMock.addJobOutputForDownload(jobOutput);

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
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));

        String jobUuid = jobClientMock.pickAJob();
        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobUuid);

        String statusUrl =
                "https://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + jobUuid + "/$status";
        assertNotNull(statusUrl);

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals("https://localhost" + API_PREFIX_V1 + FHIR_PREFIX +
                "/Group/" + contract.getContractNumber() + "/$export", startJobDTO.getUrl());
        assertNull(startJobDTO.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), startJobDTO.getOrganization());
    }

    @Test
    void testBasicPatientExportWithContract() throws Exception {
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token));

        String jobUuid = jobClientMock.pickAJob();
        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobUuid);
        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + jobUuid + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export",
                startJobDTO.getUrl());
        assertNull(startJobDTO.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), startJobDTO.getOrganization());
    }

    @Test
    void testPatientExportWithParametersWithContract() throws Exception {
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams =
                "?_type=ExplanationOfBenefit&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions =
                this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON));

        String jobUuid = jobClientMock.pickAJob();
        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobUuid);
        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + jobUuid + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams,
                startJobDTO.getUrl());
        assertEquals(EOB, startJobDTO.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), startJobDTO.getOrganization());
    }

    @Test
    void testPatientExportWithInvalidTypeWithContract() throws Exception {
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefit";
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export" + typeParams)
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
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
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
        for (int i = 0; i < MAX_JOBS_PER_CLIENT; i++) {
            this.mockMvc.perform(
                            get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().is(202));
        }
    }

    @Test
    void testPatientExportWithContractDuplicateSubmission() throws Exception {
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
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
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
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
    void testPatientExportWithContractDuplicateSubmissionDifferentContract() throws Exception {
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();

        for (int i = 0; i < MAX_JOBS_PER_CLIENT - 1; i++) {
            this.mockMvc.perform(
                            get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().is(202));
        }

        Contract contract1 = dataSetup.setupContract("Test1");

        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        assertNotNull(pdpClient);
        pdpClient.setContractId(contract1.getId());
        pdpClientRepository.saveAndFlush(pdpClient);
        Contract contractNew = dataSetup.setupContract("New Contract");

        this.mockMvc.perform(
                        get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contractNew.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    void testPatientExportWithContractDuplicateSubmissionDifferentClient() throws Exception {
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        createMaxJobsWithContract(contract);

        jobClientMock.switchAllJobsToNewOrganization("test-org");

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

    private ResultMatcher buildExpiresMatcher() {
        return result -> {
            MockHttpServletResponse response = result.getResponse();
            String headerValue = response.getHeader(EXPIRES);
            assertNotNull(headerValue, "Response does not contain header '" + EXPIRES + "'");
            OffsetDateTime actual = OffsetDateTime.parse(headerValue, RFC_1123_DATE_TIME.withZone(UTC));
            OffsetDateTime expected = OffsetDateTime.now().plusDays(EXPIRES_IN_DAYS);
            assertTrue(actual.isBefore(expected), "Sanity check that now() is larger than when this was processed");
            /*
             * 7 is the fudge factor for the tests, passing tests on my laptop.  The build environment occasionally
             * would exceed a one-second difference.
             */
            OffsetDateTime minExpected = expected.minusSeconds(7);
            assertTrue(actual.isAfter(minExpected), "Expire header time mismatch: actual - " + actual +
                    " should be greater than expected - " + minExpected);
        };
    }

    /*
     * Be more accepting of a one-second difference in timestamps when running a test.
     */
    private ResultMatcher buildTxTimeMatcher() {
        return result -> {
            OffsetDateTime buildTime = OffsetDateTime.now();
            String buildTimeStr = new org.hl7.fhir.dstu3.model.DateTimeType(buildTime.toString()).toHumanDisplay();
            String buildPlusOneStr = new org.hl7.fhir.dstu3.model.DateTimeType(buildTime.minusSeconds(1).toString()).toHumanDisplay();
            List<String> elementsToMatch = new ArrayList<>();
            elementsToMatch.add(buildTimeStr);
            elementsToMatch.add(buildPlusOneStr);

            jsonPath("$.transactionTime", IsIn.in(elementsToMatch)).match(result);
        };
    }
}