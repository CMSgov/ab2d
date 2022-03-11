package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.aggregator.AggregatorCallable;
import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.stub.PatientClaimsProcessorStub;
import gov.cms.ab2d.worker.repository.StubJobRepository;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelStubServiceImpl;
import gov.cms.ab2d.worker.util.ContractWorkerClientMock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;


import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractProcessorUnitTest {

    private static final String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    // class under test
    private ContractProcessor cut;

    @TempDir Path efsMountTmpDir;

    private StubJobRepository jobRepository;
    @Mock private CoverageDriver coverageDriver;
    @Mock private LogManager eventLogger;
    @Mock private RoundRobinBlockingQueue<PatientClaimsRequest> requestQueue;
    private PatientClaimsProcessor patientClaimsProcessor;
    private JobChannelService jobChannelService;

    private ContractDTO contract;
    private ContractForCoverageDTO contractForCoverageDTO;
    private ContractToContractCoverageMapping mapping;
    private Job job;

    private static final String STREAMING = "streaming";
    private static final String FINISHED = "finished";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        patientClaimsProcessor = spy(PatientClaimsProcessorStub.class);

        mapping = new ContractToContractCoverageMapping();
        contract = createContractDTO();
        contractForCoverageDTO = mapping.map(contract);
        PdpClient pdpClient = createClient();
        job = createJob(pdpClient);
        job.setContractNumber(contract.getContractNumber());
        jobRepository = new StubJobRepository(job);
        JobProgressServiceImpl jobProgressImpl = new JobProgressServiceImpl(jobRepository);
        jobProgressImpl.initJob(jobUuid);
        ReflectionTestUtils.setField(jobProgressImpl, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(jobProgressImpl, "reportProgressLogFrequency", 3);
        jobChannelService = new JobChannelStubServiceImpl(jobProgressImpl);
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.initialize();

        SearchConfig searchConfig = new SearchConfig(efsMountTmpDir.toFile().getAbsolutePath(),
                STREAMING, FINISHED, 0, 0, 2, 1);

        ContractWorkerClient contractWorkerClient = new ContractWorkerClientMock();
        cut = new ContractProcessorImpl(
                contractWorkerClient,
                jobRepository,
                coverageDriver,
                patientClaimsProcessor,
                eventLogger,
                requestQueue,
                jobChannelService,
                jobProgressImpl,
                mapping,
                pool,
                searchConfig);

        //ReflectionTestUtils.setField(cut, "numberPatientRequestsPerThread", 2);

        var outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        Files.createDirectories(outputDirPath);
    }

    @Test
    void testIsDone() throws IOException {
        String job = "job1";
        AggregatorCallable callable = new AggregatorCallable(efsMountTmpDir.toFile().getAbsolutePath(), job, "contract1", 200, STREAMING, FINISHED, 3);
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.initialize();
        Future<Integer> aggThread = pool.submit(callable);
        ContractProcessorImpl impl = (ContractProcessorImpl) cut;
        assertFalse(impl.isDone(aggThread, job, false));
        Path testFile = Path.of(efsMountTmpDir.toFile().getAbsolutePath(), job, FINISHED, "tst.ndjson");
        Path testFinishedDir = Path.of(efsMountTmpDir.toFile().getAbsolutePath(), job, FINISHED);
        Files.createDirectories(testFinishedDir);
        Files.createFile(testFile);
        Files.writeString(testFile, "abc");
        assertFalse(impl.isDone(aggThread, job, true));
        Files.delete(testFile);
        assertTrue(impl.isDone(aggThread, job, true));
        assertFalse(Files.exists(testFinishedDir));

        AggregatorCallable callable2 = new AggregatorCallable(efsMountTmpDir.toFile().getAbsolutePath(), job, "contract1", 200, STREAMING, FINISHED, 3);
        Future<Integer> aggThread2 = pool.submit(callable2);
        assertFalse(impl.isDone(aggThread2, job, false));
        aggThread2.cancel(true);
        assertTrue(impl.isDone(aggThread2, job, false));
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() {
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 1),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2), null));

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractDTO.class))).thenReturn(3);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 10);

        job.setStatus(JobStatus.CANCELLED);

        var exceptionThrown = assertThrows(JobCancelledException.class,
                () -> cut.process(job));

        assertTrue(exceptionThrown.getMessage().startsWith("Job was cancelled while it was being processed"));
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() {
        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractDTO.class))).thenReturn(18);
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2), null));

        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_EXPECTED, 18);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 10);

        var jobOutputs = cut.process(job);

        assertEquals(6, jobRepository.getUpdatePercentageCompletedCount());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenExpectedPatientsNotMatchActualPatientsFail() {
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 1), null));
        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractDTO.class))).thenReturn(3);

        ContractProcessingException exception = assertThrows(ContractProcessingException.class, () -> cut.process(job));

        assertTrue(exception.getMessage().contains("from database but retrieved"));
    }

    @Test
    @DisplayName("When a job has remaining requests, those remaining requests are waited on before finishing")
    void whenRemainingRequestHandlesThenAttemptToProcess() {

        try {
            ContractData contractData = new ContractData(contract, job);

            jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.FAILURE_THRESHHOLD, 20);
            jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENTS_EXPECTED, 20);

            contractData.addEobRequestHandle(new Future<>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public ProgressTrackerUpdate get() {
                    return new ProgressTrackerUpdate();
                }

                @Override
                public ProgressTrackerUpdate get(long timeout, @NotNull TimeUnit unit) {
                    return new ProgressTrackerUpdate();
                }
            }, 1);

            ReflectionTestUtils.invokeMethod(cut, "processRemainingRequests", contractData);

            assertFalse(contractData.remainingRequestHandles());
        } catch (Exception ex) {
            fail("stream helper failed", ex);
        }
    }

    @Test
    @DisplayName("When round robin blocking queue is full, patients should not be skipped")
    void whenBlockingQueueFullPatientsNotSkipped() throws InterruptedException {
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 1), new CoveragePagingRequest(1, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 1), null));

        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_EXPECTED, 2);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 1);

        when(requestQueue.size(anyString())).thenReturn(1_0000_000);

        ExecutorService singleThreadedExecutor = Executors.newSingleThreadExecutor();

        Runnable testRunnable = () -> cut.process(job);

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

    private ContractDTO createContractDTO() {
        return new ContractDTO("CONTRACT_NM_00000", "CONTRACT_00000", OffsetDateTime.now().minusDays(10), Contract.ContractType.NORMAL);
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
        job.setOrganization(pdpClient.getOrganization());
        job.setFhirVersion(STU3);
        return job;
    }

    private static List<CoverageSummary> createPatientsByContractResponse(ContractForCoverageDTO contractcoverageContractForCoverageDTO, int num) {
        FilterOutByDate.DateRange dateRange = TestUtil.getOpenRange();
        return IntStream.range(0, num).mapToObj(n -> new CoverageSummary(
                createIdentifierWithoutMbi(n),
                contractcoverageContractForCoverageDTO, List.of(dateRange)
        )).collect(toList());
    }
}