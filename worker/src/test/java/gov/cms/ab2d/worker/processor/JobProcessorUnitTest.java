package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ContractSearchEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelStubServiceImpl;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.postgresql.util.PSQLException;
import org.springframework.test.util.ReflectionTestUtils;


import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobProcessorUnitTest {
    // class under test
    private JobProcessorImpl jobProcessor;

    private static final String JOB_UUID = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @TempDir Path efsMountTmpDir;

    @Mock private FileService fileService;
    @Mock private JobRepository jobRepository;
    @Mock private JobOutputRepository jobOutputRepository;
    @Mock private ContractProcessor contractProcessor;
    @Mock private SQSEventClient eventLogger;

    private JobProgressService jobProgressService;
    private JobChannelService jobChannelService;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        JobProgressServiceImpl jobProgressUpdateService = spy(new JobProgressServiceImpl(jobRepository));
        jobProgressUpdateService.initJob(JOB_UUID);
        jobProgressService = jobProgressUpdateService;
        jobChannelService = new JobChannelStubServiceImpl(jobProgressUpdateService);

        jobProcessor = spy(new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobProgressUpdateService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                eventLogger
        ));

        ReflectionTestUtils.setField(jobProcessor, "efsMount", efsMountTmpDir.toString());

        final PdpClient pdpClient = createClient();
        job = createJob(pdpClient);

        var contract = createContract();
        job.setContractNumber(contract.getContractNumber());

        final Path outputDirPath = Paths.get(efsMountTmpDir.toString(), JOB_UUID);
        final Path outputDir = Files.createDirectories(outputDirPath);

        lenient().when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        lenient().when(fileService.createDirectory(any(Path.class))).thenReturn(outputDir);
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob_happyPath() {

        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    @Test
    @DisplayName("When client belongs to a parent sponsor, contracts for the children sponsors are processed")
    void whenTheClientBelongsToParent_ChildContractsAreProcessed() {
        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    private void doVerify() {
        verify(fileService).createDirectory(any());

        // Successful searches trigger an alert to slack
        verify(eventLogger).logAndAlert(any(JobStatusChangeEvent.class), any(List.class));
    }

    @Test
    @DisplayName("When output directory for the job already exists, delete it and create it afresh")
    void whenOutputDirectoryAlreadyExist_DeleteItAndCreateItAfresh() throws IOException{

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), JOB_UUID);
        Files.createDirectories(outputDir);

        //create files inside the directory.
        final Path filePath1 = Path.of(outputDir.toString(), "FILE_1.ndjson");
        Files.createFile(filePath1);

        final Path filePath2 = Path.of(outputDir.toString(), "FILE_2.ndjson");
        Files.createFile(filePath2);

        var errMsg = "Directory already exists";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));
        Mockito.when(fileService.createDirectory(any()))
                .thenThrow(uncheckedIOE)
                .thenReturn(efsMountTmpDir);

        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());

        verify(fileService, times(2)).createDirectory(any());
    }

    @Test
    @DisplayName("When existing output directory has a file which is not a regular file, job fails gracefully")
    void whenExistingOutputDirectoryHasSubDirectory_JobFailsGracefully() throws IOException {

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), JOB_UUID);
        Files.createDirectories(outputDir);

        //add a file in the directory which is NOT a regular file, but a directory
        final Path aDirPath = Path.of(outputDir.toString(), "DIR_1.ndjson");
        Files.createDirectories(aDirPath);
        var errMsg = "Directory already exists";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);
        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertFalse(processedJob.getStatusMessage().startsWith("Could not delete"));
        assertNull(processedJob.getExpiresAt());
    }

    @Test
    @DisplayName("When output directory creation fails due to unknown IOException, job fails gracefully")
    void whenOutputDirectoryCreationFailsDueToUnknownReason_JobFailsGracefully() {

        var errMsg = "Could not create output directory";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);

        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertTrue(processedJob.getStatusMessage().startsWith("Could not create output directory"));
        assertNull(processedJob.getExpiresAt());

        verify(fileService).createDirectory(any());
        verify(eventLogger, times(1)).logAndAlert(any(), any());
    }

    @Test
    @DisplayName("When verifying that progress tracker numbers match, then do not alert")
    void whenProgressTrackerVerificationNormal_thenNoAlerts() {
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENTS_EXPECTED, 10);
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENT_REQUEST_QUEUED, 10);
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENT_REQUESTS_PROCESSED, 10);

        jobProcessor.verifyTrackedJobProgress(job);
        verify(eventLogger, never()).alert(anyString(), any());
    }

    @Test
    @DisplayName("When verifying that progress tracker numbers do not match, then alert")
    void whenProgressTrackerVerificationFails_thenAlerts() {

        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENTS_EXPECTED, 60);
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENT_REQUEST_QUEUED, 9);
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENT_REQUESTS_PROCESSED, 9);

        jobProcessor.verifyTrackedJobProgress(job);
        verify(eventLogger, times(2)).alert(anyString(), any());
    }

    @Test
    @DisplayName("When job fails, persistence of progress tracker occurs")
    void whenJobThrowsException_thenProgressIsLogged() {
        when(contractProcessor.process(any())).thenThrow(RuntimeException.class);

        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.FAILED, processedJob.getStatus());

        verify(jobProcessor, times(1)).persistTrackedJobProgress(any());

        verify(jobProgressService, times(1)).getStatus(any());
    }

    @Test
    @DisplayName("When job succeeds, verification and persistence of progress tracker occurs")
    void whenJobSucceeds_thenProgressIsVerified() {
        var processedJob = jobProcessor.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());

        verify(jobProcessor, times(1)).verifyTrackedJobProgress(any());
        verify(jobProcessor, times(1)).persistTrackedJobProgress(any());
        verify(eventLogger, times(1)).sendLogs(any(ContractSearchEvent.class));

        // Status is pulled after finishing loading benes
        // Status is also pulled when job succeeds
        verify(jobProgressService, times(3)).getStatus(any());
    }

    @Test
    @DisplayName("Send Measure to missing listener.")
    void sendMeasureToMissingListener() {
        assertDoesNotThrow(() -> jobChannelService.sendUpdate("silly-not-a-real-guid", JobMeasure.EOBS_WRITTEN, -1));
    }

    @Test
    @DisplayName("Test to see if we match a valid extension")
    void testValidExtension(@TempDir File tempDir) throws IOException {
        Files.writeString(Path.of(tempDir.getAbsolutePath(), "file1.ndjson"), "abc");
        Files.writeString(Path.of(tempDir.getAbsolutePath(), "file2"), "def");
        Files.writeString(Path.of(tempDir.getAbsolutePath(), "file3_error.ndjson"), "ghi");
        final File[] files = tempDir.listFiles(jobProcessor.getFilenameFilter());
        assertEquals(2, files.length);
        assertNotEquals("file2", files[0].getName());
        assertNotEquals("file2", files[1].getName());
    }

    @Test
    void testJobCancelledException() {
        JobProgressServiceImpl jobProgressUpdateService = spy(new JobProgressServiceImpl(jobRepository));
        jobProgressUpdateService.initJob(JOB_UUID);
        jobProgressService = jobProgressUpdateService;
        jobChannelService = mock(JobChannelStubServiceImpl.class);

        jobProcessor = new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobProgressUpdateService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                eventLogger
        );
        ReflectionTestUtils.setField(jobProcessor, "efsMount", efsMountTmpDir.toString());

        lenient().doThrow(JobCancelledException.class).when(jobChannelService).sendUpdate(anyString(), any(), anyLong());

        assertDoesNotThrow(() -> {
            jobProcessor.process(job.getJobUuid());
        });
    }

    @Test
    void testNullPointerException() {
        JobProgressServiceImpl jobProgressUpdateService = spy(new JobProgressServiceImpl(jobRepository));
        jobProgressUpdateService.initJob(JOB_UUID);
        jobProgressService = jobProgressUpdateService;
        jobChannelService = mock(JobChannelStubServiceImpl.class);

        jobProcessor = new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobProgressUpdateService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                eventLogger
        );
        ReflectionTestUtils.setField(jobProcessor, "efsMount", efsMountTmpDir.toString());

        lenient().doThrow(NullPointerException.class).when(jobChannelService).sendUpdate(anyString(), any(), anyLong());

        assertDoesNotThrow(() -> {
            jobProcessor.process(job.getJobUuid());
        });
    }

    @Test
    void testPSQLException() {
        JobProgressServiceImpl jobProgressUpdateService = spy(new JobProgressServiceImpl(jobRepository));
        jobProgressUpdateService.initJob(JOB_UUID);
        jobProgressService = jobProgressUpdateService;
        jobChannelService = mock(JobChannelStubServiceImpl.class);

        jobProcessor = new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobProgressUpdateService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                eventLogger
        );
        ReflectionTestUtils.setField(jobProcessor, "efsMount", efsMountTmpDir.toString());

        lenient().doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                throw new PSQLException("BOOM!", null, null);
            }
        }).when(jobChannelService).sendUpdate(anyString(), any(), anyLong());

        assertDoesNotThrow(() -> {
            jobProcessor.process(job.getJobUuid());
        });
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Harry_Potter");
        pdpClient.setEnabled(TRUE);
        pdpClient.setContractId(new Contract().getId());
        return pdpClient;
    }

    private Contract createContract() {
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_NM_00000");
        contract.setContractNumber("CONTRACT_00000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));

        return contract;
    }

    private Job createJob(PdpClient pdpClient) {
        job = new Job();
        job.setJobUuid(JOB_UUID);
        job.setStatusMessage("0%");
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setOrganization(pdpClient.getOrganization());
        return job;
    }
}
