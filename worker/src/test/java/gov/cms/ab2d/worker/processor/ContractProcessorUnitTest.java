package gov.cms.ab2d.worker.processor;

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
import gov.cms.ab2d.worker.model.ContractWorkerDto;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.stub.PatientClaimsProcessorStub;
import gov.cms.ab2d.worker.repository.ContractWorkerRepository;
import gov.cms.ab2d.worker.repository.StubContractRepository;
import gov.cms.ab2d.worker.repository.StubJobRepository;
import gov.cms.ab2d.worker.service.ContractWorkerService;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelStubServiceImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    private Path outputDir;
    private ContractWorkerDto contract;
    private ContractForCoverageDTO contractForCoverageDTO;
    private ContractToContractCoverageMapping mapping;
    private Job job;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        patientClaimsProcessor = spy(PatientClaimsProcessorStub.class);

        mapping = new ContractToContractCoverageMapping();
        contract = createContract();
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

        ContractWorkerRepository contractRepository = new StubContractRepository(contract);
        cut = new ContractProcessorImpl(
                new ContractWorkerService(contractRepository),
                jobRepository,
                coverageDriver,
                patientClaimsProcessor,
                eventLogger,
                requestQueue,
                jobChannelService,
                jobProgressImpl,
                mapping);
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);


        var outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        outputDir = Files.createDirectories(outputDirPath);
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() {
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 1),
                        new CoveragePagingRequest(2, null, contractForCoverageDTO, OffsetDateTime.now())))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 2), null));

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractWorkerDto.class))).thenReturn(3);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.FAILURE_THRESHHOLD, 10);

        job.setStatus(JobStatus.CANCELLED);

        var exceptionThrown = assertThrows(JobCancelledException.class,
                () -> cut.process(outputDir, job));

        assertTrue(exceptionThrown.getMessage().startsWith("Job was cancelled while it was being processed"));
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() {
        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractWorkerDto.class))).thenReturn(18);
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

        var jobOutputs = cut.process(outputDir, job);

        assertFalse(jobOutputs.isEmpty());
        assertEquals(8, jobRepository.getUpdatePercentageCompletedCount());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenExpectedPatientsNotMatchActualPatientsFail() {
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)))
                .thenReturn(new CoveragePagingResult(createPatientsByContractResponse(contractForCoverageDTO, 1), null));
        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractWorkerDto.class))).thenReturn(3);

        ContractProcessingException exception = assertThrows(ContractProcessingException.class, () -> cut.process(outputDir, job));

        assertTrue(exception.getMessage().contains("from database but retrieved"));
    }

    @Test
    @DisplayName("When a job has remaining requests, those remaining requests are waited on before finishing")
    void whenRemainingRequestHandlesThenAttemptToProcess() {

        try (StreamHelper helper = new TextStreamHelperImpl(outputDir, contract.getContractNumber(), 200_000, 30, eventLogger, job)) {
            ContractData contractData = new ContractData(contract, job, helper);

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
                public EobSearchResult get() throws InterruptedException, ExecutionException {
                    return new EobSearchResult(job.getJobUuid(), contract.getContractNumber(), Collections.emptyList());
                }

                @Override
                public EobSearchResult get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return null;
                }
            });

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

    private ContractWorkerDto createContract() {
        ContractWorkerDto contract = new ContractWorkerDto();
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

    private static List<CoverageSummary> createPatientsByContractResponse(ContractForCoverageDTO contractcoverageContractForCoverageDTO, int num) {
        FilterOutByDate.DateRange dateRange = TestUtil.getOpenRange();
        return IntStream.range(0, num).mapToObj(n -> new CoverageSummary(
                createIdentifierWithoutMbi(n),
                contractcoverageContractForCoverageDTO, List.of(dateRange)
        )).collect(toList());
    }
}