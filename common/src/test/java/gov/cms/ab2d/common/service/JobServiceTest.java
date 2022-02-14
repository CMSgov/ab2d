package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventSummary;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.service.JobServiceImpl.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
class JobServiceTest {

    public static final String CLIENTID = "douglas.adams@towels.com";
    public static final String CONTRACT_NUMBER = "S0000";
    public static final String LOCAL_HOST = "http://localhost:8080";

    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private PdpClientService pdpClientService;

    @Value("${efs.mount}")
    private String tmpJobLocation;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JobOutputService jobOutputService;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    @Mock
    private LoggerEventSummary loggerEventSummary;

    @Mock
    private SlackLogger slackLogger;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    // Be safe and make sure nothing from another test will impact current test
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        LogManager logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger);
        jobService = new JobServiceImpl(pdpClientService, jobRepository, jobOutputService, logManager, loggerEventSummary, tmpJobLocation);
        ReflectionTestUtils.setField(jobService, "fileDownloadPath", tmpJobLocation);

        dataSetup.setupNonStandardClient(CLIENTID, CONTRACT_NUMBER, List.of());

        setupRegularClientSecurityContext();
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
    }

    private void setupRegularClientSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(CLIENTID,
                                "test", new ArrayList<>()), "pass"));
    }

    @Test
    void createJob() {
        Job job = createJobAllContracts(ZIPFORMAT);
        assertNotNull(job);
        assertNotNull(job.getId());
        assertNotNull(job.getJobUuid());
        assertNotNull(job.getOutputFormat());
        assertEquals(ZIPFORMAT, job.getOutputFormat());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals(pdpClientRepository.findByClientId(CLIENTID), job.getPdpClient());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(LOCAL_HOST, job.getRequestUrl());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(0, job.getJobOutputs().size());
        assertNull(job.getLastPollTime());
        assertNull(job.getExpiresAt());
        assertTrue(job.getJobUuid().matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"));
        assertEquals(JobStartedBy.PDP, job.getStartedBy());
        // Verify it actually got persisted in the DB
        assertEquals(job, jobRepository.findById(job.getId()).get());
    }

    @Test
    void createJobWithContract() {
        Contract contract = contractRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        Job job = jobService.createJob(EOB, LOCAL_HOST, contract.getContractNumber(), NDJSON_FIRE_CONTENT_TYPE, null, STU3);
        dataSetup.queueForCleanup(job);

        assertNotNull(job);
        assertNotNull(job.getId());
        assertNotNull(job.getJobUuid());
        assertNotNull(job.getOutputFormat());
        assertEquals(NDJSON_FIRE_CONTENT_TYPE, job.getOutputFormat());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals(pdpClientRepository.findByClientId(CLIENTID), job.getPdpClient());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(LOCAL_HOST, job.getRequestUrl());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(0, job.getJobOutputs().size());
        assertEquals(STU3, job.getFhirVersion());
        assertNull(job.getLastPollTime());
        assertNull(job.getExpiresAt());
        assertTrue(job.getJobUuid().matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"));

        assertNotNull(job.getContractNumber());
        assertEquals(job.getContractNumber(), contract.getContractNumber());

        // Verify it actually got persisted in the DB
        assertEquals(job, jobRepository.findById(job.getId()).get());
    }

    @Test
    void reportFirstJobRunForAContract() {
        Contract contract = contractRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        Job job1 = jobService.createJob(EOB, LOCAL_HOST, contract.getContractNumber(), NDJSON_FIRE_CONTENT_TYPE, null, STU3);
        dataSetup.queueForCleanup(job1);
        verify(slackLogger, times(1)).logAlert(anyString(), any());

        job1.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job1);

        Job job2 = jobService.createJob(EOB, LOCAL_HOST, contract.getContractNumber(), NDJSON_FIRE_CONTENT_TYPE, null, STU3);
        dataSetup.queueForCleanup(job2);
        verify(slackLogger, times(2)).logAlert(anyString(), any());

        job2.setStatus(JobStatus.SUCCESSFUL);
        jobRepository.saveAndFlush(job2);

        Job job3 = jobService.createJob(EOB, LOCAL_HOST, contract.getContractNumber(), NDJSON_FIRE_CONTENT_TYPE, null, STU3);
        dataSetup.queueForCleanup(job3);
        verify(slackLogger, times(2)).logAlert(anyString(), any());
    }

    @Test
    void createJobWithSpecificContractNoAttestation() {
        dataSetup.setupContractWithNoAttestation(CLIENTID, CONTRACT_NUMBER, List.of());
        assertThrows(InvalidContractException.class,
                () -> jobService.createJob(EOB, LOCAL_HOST, DataSetup.VALID_CONTRACT_NUMBER, NDJSON_FIRE_CONTENT_TYPE, null,
                        STU3));
    }

    @Test
    void createJobWithAllContractsNoAttestation() {
        dataSetup.setupContractWithNoAttestation(CLIENTID, CONTRACT_NUMBER, List.of());
        assertThrows(InvalidContractException.class,
                () -> jobService.createJob(EOB, LOCAL_HOST, null, NDJSON_FIRE_CONTENT_TYPE, null,
                        STU3));
    }

    @Test
    void failedValidation() {
        assertThrows(TransactionSystemException.class,
                () -> jobService.createJob("Patient,ExplanationOfBenefit,Coverage", LOCAL_HOST,
                        null, NDJSON_FIRE_CONTENT_TYPE, null,
                        STU3));
    }

    @Test
    void cancelJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        jobService.cancelJob(job.getJobUuid());

        // Verify that it has the correct status
        Job cancelledJob = jobRepository.findByJobUuid(job.getJobUuid());

        assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
    }

    @Test
    void cancelNonExistingJob() {
        assertThrows(ResourceNotFoundException.class,
                () -> jobService.cancelJob("NonExistingJob"));
    }

    @Test
    void getJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        Job retrievedJob = jobService.getAuthorizedJobByJobUuidAndRole(job.getJobUuid());

        assertEquals(job, retrievedJob);
    }

    @Test
    void getJobAdminRole() {
        // Job created by regular client
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        setupAdminClient();

        Job retrievedJob = jobService.getAuthorizedJobByJobUuidAndRole(job.getJobUuid());

        assertEquals(job, retrievedJob);
    }

    @Test
    void getJobCreatedByAdminRole() {
        setupAdminClient();

        // Job created by admin client
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        setupRegularClientSecurityContext();

        assertThrows(InvalidJobAccessException.class,
                () -> jobService.getAuthorizedJobByJobUuidAndRole(job.getJobUuid()));
    }

    private void setupAdminClient() {
        final String adminClient = "ADMIN_CLIENT";
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(adminClient);
        pdpClient.setOrganization(adminClient);
        pdpClient.setEnabled(true);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        pdpClient.addRole(role);

        Contract contract = dataSetup.setupContract("Y0000", AB2D_EPOCH.toOffsetDateTime());
        pdpClient.setContract(contract);

        pdpClient = pdpClientRepository.saveAndFlush(pdpClient);
        dataSetup.queueForCleanup(pdpClient);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(adminClient,
                                "test", new ArrayList<>()), "pass"));
    }

    @Test
    void getNonExistentJob() {
        assertThrows(ResourceNotFoundException.class,
                () -> jobService.getAuthorizedJobByJobUuidAndRole("NonExistent"));
    }

    @Test
    void testJobInSuccessfulState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.SUCCESSFUL);
        jobRepository.saveAndFlush(job);

        assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid()));
    }

    @Test
    public void testJobInCancelledState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid()));

    }

    @Test
    public void testJobInFailedState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(job);

        assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid()));

    }

    @Test
    void updateJob() {
        Job job = createJobAllContracts(ZIPFORMAT);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime localDateTime = OffsetDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setJobUuid("abc");
        job.setResourceTypes(EOB);
        job.setRequestUrl("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpiresAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath("error.ndjson");
        errorJobOutput.setChecksum("erroroutput");
        errorJobOutput.setFileLength(22L);
        errorJobOutput.setJob(job);

        List<JobOutput> output = List.of(jobOutput, errorJobOutput);
        job.setJobOutputs(output);

        Job updatedJob = jobService.updateJob(job);
        assertEquals(ZIPFORMAT, updatedJob.getOutputFormat());
        assertEquals(Integer.valueOf(100), updatedJob.getProgress());
        assertEquals(now, updatedJob.getLastPollTime());
        assertEquals(JobStatus.IN_PROGRESS, updatedJob.getStatus());
        assertEquals(localDateTime, updatedJob.getCreatedAt());
        assertEquals(localDateTime, updatedJob.getCompletedAt());
        assertEquals("abc", updatedJob.getJobUuid());
        assertEquals(EOB, updatedJob.getResourceTypes());
        assertEquals("http://localhost", updatedJob.getRequestUrl());
        assertEquals("Pending", updatedJob.getStatusMessage());
        assertEquals(now, updatedJob.getExpiresAt());

        JobOutput updatedOutput = updatedJob.getJobOutputs().get(0);
        assertEquals(false, updatedOutput.getError());
        assertEquals(EOB, updatedOutput.getFhirResourceType());
        assertEquals("file.ndjson", updatedOutput.getFilePath());

        JobOutput updatedErrorOutput = updatedJob.getJobOutputs().get(1);
        assertEquals(true, updatedErrorOutput.getError());
        assertEquals(OPERATION_OUTCOME, updatedErrorOutput.getFhirResourceType());
        assertEquals("error.ndjson", updatedErrorOutput.getFilePath());
    }

    @Test
    void getFileDownloadUrl() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);

        createNDJSONFile(testFile, destinationStr);
        createNDJSONFile(errorFile, destinationStr);

        Resource resource = jobService.getResourceForJob(job.getJobUuid(), testFile);
        assertEquals(testFile, resource.getFilename());

        Resource errorResource = jobService.getResourceForJob(job.getJobUuid(), errorFile);
        assertEquals(errorFile, errorResource.getFilename());
    }

    @Test
    void getJobOutputFromDifferentClient() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);

        createNDJSONFile(testFile, destinationStr);
        createNDJSONFile(errorFile, destinationStr);

        PdpClient pdpClient = new PdpClient();
        Role role = roleService.findRoleByName(SPONSOR_ROLE);
        pdpClient.setRoles(Set.of(role));
        pdpClient.setClientId("BadClient");
        pdpClient.setOrganization("BadClient");
        pdpClient.setEnabled(true);
        dataSetup.queueForCleanup(pdpClient);

        Contract contract = dataSetup.setupContract("New Contract", AB2D_EPOCH.toOffsetDateTime());
        pdpClient.setContract(contract);
        PdpClient savedPdpClient = pdpClientRepository.save(pdpClient);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(savedPdpClient.getClientId(),
                                "test", new ArrayList<>()), "pass"));

        var exceptionThrown = assertThrows(
                InvalidJobAccessException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), testFile));

        assertEquals(exceptionThrown.getMessage(), "Unauthorized");
    }

    private void createNDJSONFile(String file, String destinationStr) throws IOException {
        InputStream testFileStream = this.getClass().getResourceAsStream("/" + file);
        String fileStr = IOUtils.toString(testFileStream, StandardCharsets.UTF_8);
        try (PrintWriter out = new PrintWriter(destinationStr + File.separator + file)) {
            out.println(fileStr);
        }
    }

    private Job createJobForFileDownloads(String fileName, String errorFileName) {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime localDateTime = OffsetDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setResourceTypes(EOB);
        job.setRequestUrl("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpiresAt(now.plus(1, ChronoUnit.HOURS));
        job.setPdpClient(pdpClientService.getCurrentClient());

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath(fileName);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath(errorFileName);
        errorJobOutput.setChecksum("erroroutput");
        errorJobOutput.setFileLength(22L);
        errorJobOutput.setJob(job);

        List<JobOutput> output = List.of(jobOutput, errorJobOutput);
        job.setJobOutputs(output);

        return jobService.updateJob(job);
    }

    @Test
    void getFileDownloadUrlWithWrongFilename() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "filenamewrong.ndjson"));
    }

    @Test
    void getFileDownloadUrlWitMissingOutput() {
        String testFile = "outputmissing.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "outputmissing.ndjson"));
    }

    @Test
    void getFileDownloadAlreadyDownloaded() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        JobOutput jobOutput = job.getJobOutputs().iterator().next();
        jobOutput.setDownloaded(true);
        jobOutputRepository.save(jobOutput);

        var exception = assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "test.ndjson"));
        assertEquals("The file is not present as it has already been downloaded. Please resubmit the job.",
                exception.getMessage());
    }

    @Test
    void getFileDownloadExpired() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        job.setExpiresAt(OffsetDateTime.now().minusDays(2));
        jobRepository.save(job);


        var exception = assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "test.ndjson"));
        assertEquals("The file is not present as it has expired. Please resubmit the job.", exception.getMessage());
    }

    @Test
    void checkIfClientCanAddJobTest() {
        boolean result = jobService.checkIfCurrentClientCanAddJob();
        assertTrue(result);
    }

    @Test
    void checkIfClientCanAddJobTrueTest() {
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        boolean result = jobService.checkIfCurrentClientCanAddJob();
        assertTrue(result);
    }

    @Test
    void checkIfClientCanAddJobPastLimitTest() {
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        boolean result = jobService.checkIfCurrentClientCanAddJob();
        assertFalse(result);
    }

    @Test
    void deleteFileForJobTest() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        jobService.deleteFileForJob(new File(testFile), job.getJobUuid());
    }

    private Job createJobAllContracts(String outputFormat) {
        Job job = jobService.createJob(EOB, LOCAL_HOST, null, outputFormat, null,
                STU3);
        dataSetup.queueForCleanup(job);
        return job;
    }
}
