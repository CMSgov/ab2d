package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverStub;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobProcessorUnitTest {
    // class under test
    private JobProcessor cut;

    private static final String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @TempDir Path efsMountTmpDir;

    @Mock private FileService fileService;
    @Mock private JobRepository jobRepository;
    @Mock private JobOutputRepository jobOutputRepository;
    @Mock private ContractProcessor contractProcessor;
    @Mock private LogManager eventLogger;

    private CoverageDriverStub coverageDriver;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        JobProgressService jobProgressService = new JobProgressServiceImpl(jobRepository);
        JobChannelService jobChannelService = new JobChannelServiceImpl(jobProgressService);

        coverageDriver = spy(new CoverageDriverStub(10, 20));
        cut = new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                coverageDriver,
                eventLogger
        );

        ReflectionTestUtils.setField(cut, "efsMount", efsMountTmpDir.toString());

        final PdpClient pdpClient = createClient();
        job = createJob(pdpClient);

        var contract = createContract();
        job.setContract(contract);

        final Path outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        final Path outputDir = Files.createDirectories(outputDirPath);

        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        lenient().when(fileService.createDirectory(any(Path.class))).thenReturn(outputDir);
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob_happyPath() {

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    @Test
    @DisplayName("When client belongs to a parent sponsor, contracts for the children sponsors are processed")
    void whenTheClientBelongsToParent_ChildContractsAreProcessed() {
        job.getPdpClient();

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    private void doVerify() {
        verify(fileService).createDirectory(any());
        verify(coverageDriver).pageCoverage(any(Job.class));
        verify(coverageDriver).pageCoverage(any(CoveragePagingRequest.class));
    }

    @Test
    @DisplayName("When output directory for the job already exists, delete it and create it afresh")
    void whenOutputDirectoryAlreadyExist_DeleteItAndCreateItAfresh() throws IOException{

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), jobUuid);
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

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());

        verify(fileService, times(2)).createDirectory(any());
        verify(coverageDriver).pageCoverage(any(Job.class));
        verify(coverageDriver).pageCoverage(any(CoveragePagingRequest.class));
    }

    @Test
    @DisplayName("When existing output directory has a file which is not a regular file, job fails gracefully")
    void whenExistingOutputDirectoryHasSubDirectory_JobFailsGracefully() throws IOException {

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), jobUuid);
        Files.createDirectories(outputDir);

        //add a file in the directory which is NOT a regular file, but a directory
        final Path aDirPath = Path.of(outputDir.toString(), "DIR_1.ndjson");
        Files.createDirectories(aDirPath);
        var errMsg = "Directory already exists";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);
        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertTrue(processedJob.getStatusMessage().startsWith("Could not delete"));
        assertNull(processedJob.getExpiresAt());

        verify(fileService).createDirectory(any());
        verify(coverageDriver, never()).pageCoverage(any(Job.class));
    }

    @Test
    @DisplayName("When output directory creation fails due to unknown IOException, job fails gracefully")
    void whenOutputDirectoryCreationFailsDueToUnknownReason_JobFailsGracefully() {

        var errMsg = "Could not create output directory";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertTrue(processedJob.getStatusMessage().startsWith("Could not create output directory"));
        assertNull(processedJob.getExpiresAt());

        verify(fileService).createDirectory(any());
        verify(coverageDriver, never()).pageCoverage(any(Job.class));
        verify(eventLogger, times(1)).logAndAlert(any(), any());
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Harry_Potter");
        pdpClient.setEnabled(TRUE);
        pdpClient.setContract(createContract());
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
        Job job = new Job();
        job.setJobUuid(jobUuid);
        job.setStatusMessage("0%");
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setPdpClient(pdpClient);
        return job;
    }
}