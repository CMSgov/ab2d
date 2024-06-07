package gov.cms.ab2d.job.service;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.RoleService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.JobTestSpringBootApp;
import gov.cms.ab2d.job.dto.JobPollResult;
import gov.cms.ab2d.job.dto.StaleJob;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStartedBy;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.job.repository.JobRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.OPERATION_OUTCOME;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.job.model.JobStatus.FAILED;
import static gov.cms.ab2d.job.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.job.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = JobTestSpringBootApp.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JobServiceTest extends JobCleanup {

    public static final String CLIENTID = "douglas.adams@towels.com";
    public static final String CONTRACT_NUMBER = "S0000";
    public static final String LOCAL_HOST = "http://localhost:8080";

    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private ContractServiceStub contractServiceStub;

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
    private ContractService contractService;

    @Autowired
    private SQSEventClient sqsEventClient;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    // Be safe and make sure nothing from another test will impact current test
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        jobService = new JobServiceImpl(jobRepository, jobOutputService, sqsEventClient, tmpJobLocation);
        ReflectionTestUtils.setField(jobService, "fileDownloadPath", tmpJobLocation);

        dataSetup.setupNonStandardClient(CLIENTID, CONTRACT_NUMBER, of());

        setupRegularClientSecurityContext();
    }

    @AfterEach
    public void cleanup() {
        jobCleanup();
        dataSetup.cleanup();
    }

    static void setupRegularClientSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(CLIENTID,
                                "test", new ArrayList<>()), "pass"));
    }

    private StartJobDTO buildStartJobContract(String contractNumber) {
        return buildStartJob(contractNumber, EOB, NDJSON_FIRE_CONTENT_TYPE);
    }

    private StartJobDTO buildStartJobOutputFormat(String outputFormat) {
        return buildStartJob(contractService.getContractByContractId(pdpClientService.getCurrentClient().getContractId()).getContractNumber(), EOB, outputFormat);
    }

    private StartJobDTO buildStartJobResourceTypes(String resourceTypes) {
        return buildStartJob(contractService.getContractByContractId(pdpClientService.getCurrentClient().getContractId()).getContractNumber(),
                resourceTypes, NDJSON_FIRE_CONTENT_TYPE);
    }

    private StartJobDTO buildStartJob(String contractNumber, String resourceTypes, String outputFormat) {
        String organization = pdpClientService.getCurrentClient().getOrganization();
        return new StartJobDTO(contractNumber, organization, resourceTypes, LOCAL_HOST, outputFormat, null, null, STU3);
    }

    @Test
    void createJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        assertNotNull(job);
        assertNotNull(job.getId());
        assertNotNull(job.getJobUuid());
        assertNotNull(job.getOutputFormat());
        assertEquals(NDJSON_FIRE_CONTENT_TYPE, job.getOutputFormat());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals(pdpClientRepository.findByClientId(CLIENTID).getOrganization(), job.getOrganization());
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
        Contract contract = contractServiceStub.getAllAttestedContracts().iterator().next();

        Job job = jobService.createJob(buildStartJobContract(contract.getContractNumber()));
        addJobForCleanup(job);

        assertNotNull(job);
        assertNotNull(job.getId());
        assertNotNull(job.getJobUuid());
        assertNotNull(job.getOutputFormat());
        assertEquals(NDJSON_FIRE_CONTENT_TYPE, job.getOutputFormat());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals(pdpClientRepository.findByClientId(CLIENTID).getOrganization(), job.getOrganization());
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

        JobPollResult jobPollResult = jobService.poll(false, job.getJobUuid(), job.getOrganization(), 0);
        assertNotNull(jobPollResult);
        assertEquals(JobStatus.SUBMITTED, jobPollResult.getStatus());
    }

    @Test
    void reportFirstJobRunForAContract() {
        Contract contract = contractServiceStub.getAllAttestedContracts().iterator().next();

        Job job1 = jobService.createJob(buildStartJobContract(contract.getContractNumber()));
        addJobForCleanup(job1);
        verify(sqsEventClient, times(1)).sendLogs(any());

        job1.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job1);

        Job job2 = jobService.createJob(buildStartJobContract(contract.getContractNumber()));
        addJobForCleanup(job2);
        verify(sqsEventClient, times(2)).sendLogs(any());

        job2.setStatus(SUCCESSFUL);
        jobRepository.saveAndFlush(job2);

        Job job3 = jobService.createJob(buildStartJobContract(contract.getContractNumber()));
        addJobForCleanup(job3);
        verify(sqsEventClient, times(3)).sendLogs(any());
    }

    @Test
    void failedValidation() {
        assertThrows(TransactionSystemException.class,
                () -> jobService.createJob(buildStartJobResourceTypes("Patient,ExplanationOfBenefit,Coverage")));
    }

    @Test
    void cancelJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        jobService.cancelJob(job.getJobUuid(), pdpClientService.getCurrentClient().getOrganization());

        // Verify that it has the correct status
        Job cancelledJob = jobRepository.findByJobUuid(job.getJobUuid());

        assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
    }

    @Test
    void cancelNonExistingJob() {
        assertThrows(ResourceNotFoundException.class,
                () -> jobService.cancelJob("NonExistingJob", pdpClientService.getCurrentClient().getOrganization()));
    }

    @Test
    void getJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        Job retrievedJob = jobService.getAuthorizedJobByJobUuid(job.getJobUuid(),
                pdpClientService.getCurrentClient().getOrganization());

        assertEquals(job, retrievedJob);
    }

    @Test
    void getJobAdminRole() {
        // Job created by regular client
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        setupAdminClient();

        assertTrue(pdpClientService.getCurrentClient().isAdmin());
        Job retrievedJob = jobService.getJobByJobUuid(job.getJobUuid());

        assertEquals(job, retrievedJob);
    }

    @Test
    void getJobCreatedByAdminRole() {
        setupAdminClient();

        // Job created by admin client
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        setupRegularClientSecurityContext();

        assertThrows(InvalidJobAccessException.class,
                () -> jobService.getAuthorizedJobByJobUuid(job.getJobUuid(),
                        pdpClientService.getCurrentClient().getOrganization()));
    }

    private void setupAdminClient() {
        final String adminClient = "ADMIN_CLIENT";
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(adminClient);
        pdpClient.setOrganization(adminClient);
        pdpClient.setEnabled(true);
        Role role = roleService.findRoleByName(Role.ADMIN_ROLE);
        pdpClient.addRole(role);

        Contract contract = dataSetup.setupContract("Y0000", AB2D_EPOCH.toOffsetDateTime());
        pdpClient.setContractId(contract.getId());

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
                () -> jobService.getAuthorizedJobByJobUuid("NonExistent",
                        pdpClientService.getCurrentClient().getOrganization()));
    }

    @Test
    void testJobInSuccessfulState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(SUCCESSFUL);
        jobRepository.saveAndFlush(job);

        assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid(),
                pdpClientService.getCurrentClient().getOrganization()));
    }

    @Test
    void testJobInCancelledState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid(),
                pdpClientService.getCurrentClient().getOrganization()));

    }

    @Test
    void testJobInFailedState() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(FAILED);
        jobRepository.saveAndFlush(job);

        assertThrows(InvalidJobStateTransition.class, () -> jobService.cancelJob(job.getJobUuid(),
                pdpClientService.getCurrentClient().getOrganization()));

    }

    @Test
    void updateJob() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
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

        List<JobOutput> output = of(jobOutput, errorJobOutput);
        job.setJobOutputs(output);

        Job updatedJob = jobService.updateJob(job);
        assertEquals(NDJSON_FIRE_CONTENT_TYPE, updatedJob.getOutputFormat());
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

        Resource resource = jobService.getResourceForJob(job.getJobUuid(), testFile,
                pdpClientService.getCurrentClient().getOrganization());
        assertEquals(testFile, resource.getFilename());

        Resource errorResource = jobService.getResourceForJob(job.getJobUuid(), errorFile,
                pdpClientService.getCurrentClient().getOrganization());
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
        Role role = roleService.findRoleByName(Role.SPONSOR_ROLE);
        pdpClient.setRoles(Set.of(role));
        pdpClient.setClientId("BadClient");
        pdpClient.setOrganization("BadClient");
        pdpClient.setEnabled(true);
        dataSetup.queueForCleanup(pdpClient);

        Contract contract = dataSetup.setupContract("New Contract", AB2D_EPOCH.toOffsetDateTime());
        pdpClient.setContractId(contract.getId());
        PdpClient savedPdpClient = pdpClientRepository.save(pdpClient);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(savedPdpClient.getClientId(),
                                "test", new ArrayList<>()), "pass"));

        var exceptionThrown = assertThrows(
                InvalidJobAccessException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), testFile,
                        pdpClientService.getCurrentClient().getOrganization()));

        assertEquals("Unauthorized", exceptionThrown.getMessage());
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
        job.setOrganization(pdpClientService.getCurrentClient().getOrganization());

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

        List<JobOutput> output = of(jobOutput, errorJobOutput);
        job.setJobOutputs(output);

        return jobService.updateJob(job);
    }

    @Test
    void getFileDownloadUrlWithWrongFilename() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "filenamewrong.ndjson",
                        pdpClientService.getCurrentClient().getOrganization()));
    }

    @Test
    void getFileDownloadUrlWitMissingOutput() {
        String testFile = "outputmissing.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "outputmissing.ndjson",
                        pdpClientService.getCurrentClient().getOrganization()));
    }

    @Test
    void getFileDownloadAlreadyDownloadedMaxTimes() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        JobOutput jobOutput = job.getJobOutputs().iterator().next();
        jobOutput.setDownloaded(30);
        jobOutputRepository.save(jobOutput);

        var exception = assertThrows(JobOutputMissingException.class,
                () -> jobService.getResourceForJob(job.getJobUuid(), "test.ndjson",
                        pdpClientService.getCurrentClient().getOrganization()));
        assertEquals("The file has already been download the maximum number of allowed times.",
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
                () -> jobService.getResourceForJob(job.getJobUuid(), "test.ndjson",
                        pdpClientService.getCurrentClient().getOrganization()));
        assertEquals("The file is not present as it has expired. Please resubmit the job.", exception.getMessage());
    }

    private boolean canRun() {
        PdpClient pdpClient = pdpClientService.getCurrentClient();
        return jobService.activeJobs(pdpClient.getOrganization()) < pdpClient.getMaxParallelJobs();
    }

    @Test
    void checkIfClientCanAddJobTest() {
        assertTrue(canRun());
    }

    @Test
    void checkIfClientCanAddJobTrueTest() {
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        assertTrue(canRun());
    }

    @Test
    void checkIfClientCanAddJobPastLimitTest() {
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);

        assertFalse(canRun());
    }

    @Test
    void incrementDownload() {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);
        jobService.incrementDownload(new File(testFile), job.getJobUuid());
        assertEquals(1, jobOutputService.findByFilePathAndJob(testFile, job).getDownloaded());
    }

    private Job createJobAllContracts(String outputFormat) {
        Job job = jobService.createJob(buildStartJobOutputFormat(outputFormat));
        addJobForCleanup(job);
        return job;
    }

    @Test
    void checkForExpirations() {
        Job job = createJobAllContracts(NDJSON_FIRE_CONTENT_TYPE);
        job.setStatus(FAILED);
        jobRepository.save(job);

        List<StaleJob> staleJobs = jobService.checkForExpiration(of(job.getJobUuid()), 1);
        assertFalse(staleJobs.isEmpty());
        assertEquals(1, staleJobs.size());

        staleJobs = jobService.checkForExpiration(Collections.emptyList(), 1);
        assertTrue(staleJobs.isEmpty());
    }
}
