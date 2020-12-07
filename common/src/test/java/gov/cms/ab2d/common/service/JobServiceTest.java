package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.reports.sql.DoSummary;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
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
import static gov.cms.ab2d.common.util.DataSetup.TEST_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class JobServiceTest {

    public static final String LOCAL_HOST = "http://localhost:8080";

    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private UserService userService;

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
    private DoSummary doSummary;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    // Be safe and make sure nothing from another test will impact current test
    @BeforeEach
    public void setup() {
        LogManager logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        jobService = new JobServiceImpl(userService, jobRepository, jobOutputService, logManager, doSummary);
        ReflectionTestUtils.setField(jobService, "fileDownloadPath", tmpJobLocation);

        // todo: Very bizarre behavior happens if these are moved to an @AfterEach method instead.  Doing deleteAll()
        // in a setup method is definitely a code smell.
        jobRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();

        dataSetup.setupUser(List.of());

        setupRegularUserSecurityContext();
    }

    @Test
    public void createJob() {
        Job job = createJobAllContracts(ZIPFORMAT);
        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobUuid()).isNotNull();
        assertThat(job.getOutputFormat()).isNotNull();
        assertEquals(job.getOutputFormat(), ZIPFORMAT);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getRequestUrl(), LOCAL_HOST);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getJobOutputs().size(), 0);
        assertNull(job.getLastPollTime());
        assertNull(job.getExpiresAt());
        assertThat(job.getJobUuid()).matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test
    public void createJobWithContract() {
        Contract contract = contractRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        Job job = jobService.createJob(EOB, LOCAL_HOST, contract.getContractNumber(), NDJSON_FIRE_CONTENT_TYPE, null);
        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobUuid()).isNotNull();
        assertThat(job.getOutputFormat()).isNotNull();
        assertEquals(NDJSON_FIRE_CONTENT_TYPE, job.getOutputFormat());
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getRequestUrl(), LOCAL_HOST);
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getJobOutputs().size(), 0);
        assertNull(job.getLastPollTime());
        assertNull(job.getExpiresAt());
        assertThat(job.getJobUuid()).matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
        assertNotNull(job.getContract());
        assertEquals(job.getContract().getContractNumber(), contract.getContractNumber());

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test
    public void createJobWithBadContract() {
        Assertions.assertThrows(ContractNotFoundException.class,
                () -> jobService.createJob(EOB, LOCAL_HOST, "BadContract", NDJSON_FIRE_CONTENT_TYPE, null));
    }

    @Test
    public void createJobWithSpecificContractNoAttestation() {
        dataSetup.setupContractWithNoAttestation(List.of());
        Assertions.assertThrows(InvalidContractException.class,
                () -> jobService.createJob(EOB, LOCAL_HOST, DataSetup.VALID_CONTRACT_NUMBER, NDJSON_FIRE_CONTENT_TYPE, null));
    }

    @Test
    public void createJobWithAllContractsNoAttestation() {
        dataSetup.setupContractWithNoAttestation(List.of());
        Assertions.assertThrows(InvalidContractException.class,
                () -> jobService.createJob(EOB, LOCAL_HOST, null, NDJSON_FIRE_CONTENT_TYPE, null));
    }

    @Test
    public void failedValidation() {
        Assertions.assertThrows(TransactionSystemException.class,
                () -> jobService.createJob("Patient,ExplanationOfBenefit,Coverage", LOCAL_HOST,
                        null, NDJSON_FIRE_CONTENT_TYPE, null));
    }

    @Test
    public void cancelJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        jobService.cancelJob(job.getJobUuid());

        // Verify that it has the correct status
        Job cancelledJob = jobRepository.findByJobUuid(job.getJobUuid());

        assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
    }

    @Test
    public void cancelNonExistingJob() {
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> jobService.cancelJob("NonExistingJob"));
    }

    @Test
    public void getJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        Job retrievedJob = jobService.getAuthorizedJobByJobUuidAndRole(job.getJobUuid());

        assertEquals(job, retrievedJob);
    }

    @Test
    public void getJobAdminRole() {
        // Job created by regular user
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        setupAdminUser();

        Job retrievedJob = jobService.getAuthorizedJobByJobUuidAndRole(job.getJobUuid());

        assertEquals(job, retrievedJob);
    }

    @Test
    public void getJobCreatedByAdminRole() {
        setupAdminUser();

        // Job created by admin user
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        setupRegularUserSecurityContext();

        Assertions.assertThrows(InvalidJobAccessException.class,
                () -> jobService.getAuthorizedJobByJobUuidAndRole(job.getJobUuid()));
    }

    private void setupAdminUser() {
        final String adminUsername = "ADMIN_USER";
        User user = new User();
        user.setUsername(adminUsername);
        user.setEnabled(true);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        user.addRole(role);

        Contract contract = dataSetup.setupContract("Y0000");
        user.setContract(contract);

        userRepository.saveAndFlush(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(adminUsername,
                                "test", new ArrayList<>()), "pass"));
    }

    private void setupRegularUserSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(TEST_USER,
                                "test", new ArrayList<>()), "pass"));
    }

    @Test
    public void getNonExistentJob() {
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> jobService.getAuthorizedJobByJobUuidAndRole("NonExistent"));
    }

    @Test
    public void testJobInSuccessfulState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.SUCCESSFUL);
        jobRepository.saveAndFlush(job);

        Assertions.assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid()));
    }

    @Test
    public void testJobInCancelledState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        Assertions.assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid()));

    }

    @Test
    public void testJobInFailedState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(job);

        Assertions.assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid()));

    }

    @Test
    public void updateJob() {
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
        Assert.assertEquals(ZIPFORMAT, updatedJob.getOutputFormat());
        Assert.assertEquals(Integer.valueOf(100), updatedJob.getProgress());
        Assert.assertEquals(now, updatedJob.getLastPollTime());
        Assert.assertEquals(JobStatus.IN_PROGRESS, updatedJob.getStatus());
        Assert.assertEquals(localDateTime, updatedJob.getCreatedAt());
        Assert.assertEquals(localDateTime, updatedJob.getCompletedAt());
        Assert.assertEquals("abc", updatedJob.getJobUuid());
        Assert.assertEquals(EOB, updatedJob.getResourceTypes());
        Assert.assertEquals("http://localhost", updatedJob.getRequestUrl());
        Assert.assertEquals("Pending", updatedJob.getStatusMessage());
        Assert.assertEquals(now, updatedJob.getExpiresAt());

        JobOutput updatedOutput = updatedJob.getJobOutputs().get(0);
        Assert.assertEquals(false, updatedOutput.getError());
        Assert.assertEquals(EOB, updatedOutput.getFhirResourceType());
        Assert.assertEquals("file.ndjson", updatedOutput.getFilePath());

        JobOutput updatedErrorOutput = updatedJob.getJobOutputs().get(1);
        Assert.assertEquals(true, updatedErrorOutput.getError());
        Assert.assertEquals(OPERATION_OUTCOME, updatedErrorOutput.getFhirResourceType());
        Assert.assertEquals("error.ndjson", updatedErrorOutput.getFilePath());
    }

    @Test
    public void getFileDownloadUrl() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);

        createNDJSONFile(testFile, destinationStr);
        createNDJSONFile(errorFile, destinationStr);

        Resource resource = jobService.getResourceForJob(job.getJobUuid(), testFile);
        Assert.assertEquals(testFile, resource.getFilename());

        Resource errorResource = jobService.getResourceForJob(job.getJobUuid(), errorFile);
        Assert.assertEquals(errorFile, errorResource.getFilename());
    }

    @Test
    public void getJobOutputFromDifferentUser() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);

        createNDJSONFile(testFile, destinationStr);
        createNDJSONFile(errorFile, destinationStr);

        User user = new User();
        Role role = roleService.findRoleByName("SPONSOR");
        user.setRoles(Set.of(role));
        user.setUsername("BadUser");
        user.setEnabled(true);

        Contract contract = dataSetup.setupContract("New Contract");
        user.setContract(contract);
        User savedUser = userRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(savedUser.getUsername(),
                                "test", new ArrayList<>()), "pass"));

        var exceptionThrown = assertThrows(
                InvalidJobAccessException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), testFile));

        Assert.assertEquals(exceptionThrown.getMessage(), "Unauthorized");
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
        job.setUser(userService.getCurrentUser());

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
    public void getFileDownloadUrlWithWrongFilename() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "filenamewrong.ndjson"));
    }

    @Test
    public void getFileDownloadUrlWitMissingOutput() {
        String testFile = "outputmissing.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Assertions.assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "outputmissing.ndjson"));
    }

    @Test
    public void getFileDownloadAlreadyDownloaded() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        JobOutput jobOutput = job.getJobOutputs().iterator().next();
        jobOutput.setDownloaded(true);
        jobOutputRepository.save(jobOutput);


        var exception = Assertions.assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "test.ndjson"));
        assertEquals("The file is not present as it has already been downloaded. Please resubmit the job.",
                exception.getMessage());
    }

    @Test
    public void getFileDownloadExpired() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        job.setExpiresAt(OffsetDateTime.now().minusDays(2));
        jobRepository.save(job);


        var exception = Assertions.assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "test.ndjson"));
        assertEquals("The file is not present as it has expired. Please resubmit the job.", exception.getMessage());
    }

    @Test
    public void checkIfUserCanAddJobTest() {
        boolean result = jobService.checkIfCurrentUserCanAddJob();
        Assert.assertTrue(result);
    }

    @Test
    public void checkIfUserCanAddJobTrueTest() {
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        boolean result = jobService.checkIfCurrentUserCanAddJob();
        Assert.assertTrue(result);
    }

    @Test
    public void checkIfUserCanAddJobPastLimitTest() {
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        boolean result = jobService.checkIfCurrentUserCanAddJob();
        Assert.assertFalse(result);
    }

    @Test
    public void deleteFileForJobTest() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        jobService.deleteFileForJob(new File(testFile), job.getJobUuid());
    }

    private Job createJobAllContracts(String outputFormat) {
        return jobService.createJob(EOB, LOCAL_HOST, null, outputFormat, null);
    }
}
