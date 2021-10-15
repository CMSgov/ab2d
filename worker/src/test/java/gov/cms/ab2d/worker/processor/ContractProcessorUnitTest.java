package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.stub.PatientClaimsProcessorStub;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelStubServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractProcessorUnitTest {

    private static final String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    // class under test
    private ContractProcessor cut;

    @TempDir Path efsMountTmpDir;

    @Mock private JobRepository jobRepository;
    @Mock private CoverageDriver coverageDriver;
    @Mock private LogManager eventLogger;
    @Mock private RoundRobinBlockingQueue<PatientClaimsRequest> requestQueue;
    private PatientClaimsProcessor patientClaimsProcessor;
    private JobChannelService jobChannelService;

    private Path outputDir;
    private Contract contract;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        patientClaimsProcessor = spy(PatientClaimsProcessorStub.class);

        JobProgressServiceImpl jobProgressImpl = new JobProgressServiceImpl(jobRepository);
        jobProgressImpl.initJob(jobUuid);
        ReflectionTestUtils.setField(jobProgressImpl, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(jobProgressImpl, "reportProgressLogFrequency", 3);
        jobChannelService = new JobChannelStubServiceImpl(jobProgressImpl);

        cut = new ContractProcessorImpl(
                jobRepository,
                coverageDriver,
                patientClaimsProcessor,
                eventLogger,
                requestQueue,
                jobChannelService,
                jobProgressImpl);
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);

        PdpClient pdpClient = createClient();
        job = createJob(pdpClient);
        contract = createContract();
        job.setContract(contract);

        var outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        outputDir = Files.createDirectories(outputDirPath);
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() {

        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 1),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2), null));

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class))).thenReturn(3);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_EXPECTED, 3);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 10);

        when(jobRepository.findJobStatus(anyString())).thenReturn(JobStatus.CANCELLED);

        var exceptionThrown = assertThrows(JobCancelledException.class,
                () -> cut.process(outputDir, job));

        assertTrue(exceptionThrown.getMessage().startsWith("Job was cancelled while it was being processed"));
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() {
        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class))).thenReturn(18);
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2),
                        new CoveragePagingRequest(2, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 2), null));

        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_EXPECTED, 18);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 10);

        var jobOutputs = cut.process(outputDir, job);

        assertFalse(jobOutputs.isEmpty());
        verify(jobRepository, times(8)).updatePercentageCompleted(anyString(), anyInt());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When round robin blocking queue is full, patients should not be skipped")
    void whenBlockingQueueFullPatientsNotSkipped() throws InterruptedException {

        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 1), new CoveragePagingRequest(1, null, contract, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contract, 1), null));

        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_EXPECTED, 2);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 1);

        when(requestQueue.size(anyString())).thenReturn(1_0000_000);

        ExecutorService singleThreadedExecutor = Executors.newSingleThreadExecutor();

        Runnable testRunnable = () -> cut.process(outputDir, job);

        Future<?> future = singleThreadedExecutor.submit(testRunnable);

        Thread.sleep(5000);

        assertFalse(future.isDone());

        future.cancel(true);
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
        job.setFhirVersion(STU3);
        return job;
    }

    private static List<CoverageSummary> createPatientsByContractResponse(Contract contract, int num) {
        FilterOutByDate.DateRange dateRange = TestUtil.getOpenRange();
        return IntStream.range(0, num).mapToObj(n -> new CoverageSummary(
                createIdentifierWithoutMbi(n),
                contract, List.of(dateRange)
        )).collect(toList());
    }
}