package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ContractSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.util.HealthCheck;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.TestUtil.getOpenRange;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
class JobProcessorIntegrationTest {

    private static final String CONTRACT_NAME = "CONTRACT_0000";
    private static final String CONTRACT_NUMBER = "CONTRACT_0000";
    private static final String JOB_UUID = "S0000";
    public static final String COMPLETED_PERCENT = "100%";

    private JobProcessor cut;       // class under test

    @Autowired
    private FileService fileService;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobChannelService jobChannelService;

    @Autowired
    private JobProgressService jobProgressService;

    @Autowired
    private JobProgressUpdateService jobProgressUpdateService;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Autowired
    private RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Autowired
    private HealthCheck healthCheck;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private ContractToContractCoverageMapping mapping;

    @Mock
    private CoverageDriver mockCoverageDriver;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    @Mock
    private SlackLogger slackLogger;

    @Mock
    private BFDClient mockBfdClient;

    @TempDir
    File tmpEfsMountDir;

    private Job job;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();
    private static final ExplanationOfBenefit EOB = (ExplanationOfBenefit) EobTestDataUtil.createEOB();

    private ContractWorkerDto contract;
    private ContractForCoverageDTO contractForCoverageDTO;
    private RuntimeException fail;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        LogManager logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger);
        PdpClient pdpClient = createClient();

        contract = createContract();
        contractForCoverageDTO = mapping.map(contract);
        fail = new RuntimeException("TEST EXCEPTION");

        job = createJob(pdpClient);
        job.setContractNumber(contract.getContractNumber());
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.saveAndFlush(job);

        when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong())).thenAnswer((args) -> {
            ExplanationOfBenefit copy = EOB.copy();
            copy.getPatient().setReference("Patient/" + args.getArgument(1));
            return EobTestDataUtil.createBundle(copy);
        });
        when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong(), any())).thenAnswer((args) -> {
            ExplanationOfBenefit copy = EOB.copy();
            copy.getPatient().setReference("Patient/" + args.getArgument(1));
            return EobTestDataUtil.createBundle(copy);
        });

        when(mockCoverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(Contract.class))).thenReturn(100);

        when(mockCoverageDriver.pageCoverage(any(CoveragePagingRequest.class))).thenReturn(
                new CoveragePagingResult(loadFauxMetadata(contractForCoverageDTO, 99), null));

        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, logManager);
        ReflectionTestUtils.setField(patientClaimsProcessor, "earliestDataDate", "01/01/1900");
        ContractProcessor contractProcessor = new ContractProcessorImpl(
                contractRepository,
                jobRepository,
                mockCoverageDriver,
                patientClaimsProcessor,
                logManager,
                eobClaimRequestsQueue,
                jobChannelService,
                jobProgressService,
                mapping);


        cut = new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobProgressUpdateService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                logManager
        );

        ReflectionTestUtils.setField(cut, "efsMount", tmpEfsMountDir.toString());
        ReflectionTestUtils.setField(cut, "failureThreshold", 10);
    }

    @AfterEach
    void cleanup() {
        loggerEventRepository.delete();
        dataSetup.cleanup();
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob() {
        var processedJob = cut.process(job.getJobUuid());

        List<LoggableEvent> jobStatusChange = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals(JobStatus.SUCCESSFUL.name(), jobEvent.getNewStatus());
        assertEquals(JobStatus.IN_PROGRESS.name(), jobEvent.getOldStatus());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());
    }

    @Test
    @DisplayName("When a job has not benes, still generate a contract search event")
    void whenJobHasNoBenes_stillGenerateContractSearchEvent() {
        var processedJob = cut.process(job.getJobUuid());

        final List<JobOutput> jobOutputs = processedJob.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());

        // Always generate a contract search event
        assertFalse(loggerEventRepository.load(ContractSearchEvent.class).isEmpty());
    }

    @Test
    @DisplayName("When a job is in submitted by the parent client, it process the contracts for the children")
    void whenJobSubmittedByParentClient_ProcessAllContractsForChildrenSponsors() {
        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        final List<JobOutput> jobOutputs = processedJob.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());

        // Always generate a contract search event
        assertFalse(loggerEventRepository.load(ContractSearchEvent.class).isEmpty());
    }

    @Test
    @DisplayName("When bene has no eobs then do not count bene toward statistic")
    void when_beneHasNoEobs_notCounted() {
        reset(mockBfdClient);
        OngoingStubbing<IBaseBundle> stubbing = when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong(), any()));
        stubbing = andThenAnswerEobs(stubbing, 0, 95);
        stubbing.thenReturn(BundleUtils.createBundle())
                .thenReturn(BundleUtils.createBundle())
                .thenReturn(BundleUtils.createBundle())
                .thenReturn(BundleUtils.createBundle())
                .thenReturn(BundleUtils.createBundle());

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        List<LoggableEvent> beneSearchEvents = loggerEventRepository.load(ContractSearchEvent.class);
        assertEquals(1, beneSearchEvents.size());
        ContractSearchEvent event = (ContractSearchEvent) beneSearchEvents.get(0);
        assertEquals(JOB_UUID, event.getJobId());
        assertEquals(100, event.getBenesExpected());
        assertEquals(100, event.getBenesSearched());
        assertEquals(CONTRACT_NAME, event.getContractNumber());
            assertEquals(95, event.getBenesWithEobs());

        final List<JobOutput> jobOutputs = processedJob.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    @DisplayName("When the error count is below threshold, job does not fail")
    void when_errorCount_is_below_threshold_do_not_fail_job() {
        reset(mockBfdClient);
        OngoingStubbing<IBaseBundle> stubbing = when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong(), any()));
        stubbing = andThenAnswerEobs(stubbing, 0, 95);
        stubbing.thenThrow(fail, fail, fail, fail, fail);

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        List<LoggableEvent> beneSearchEvents = loggerEventRepository.load(ContractSearchEvent.class);
        assertEquals(1, beneSearchEvents.size());
        ContractSearchEvent event = (ContractSearchEvent) beneSearchEvents.get(0);
        assertEquals(JOB_UUID, event.getJobId());
        assertEquals(100, event.getBenesExpected());
        assertEquals(100, event.getBenesSearched());
        assertEquals(CONTRACT_NAME, event.getContractNumber());
        assertEquals(95, event.getBenesWithEobs());

        final List<JobOutput> jobOutputs = processedJob.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    void testHealth() {
        assertTrue(healthCheck.healthy());
    }

    @Test
    @DisplayName("When the error count is greater than or equal to threshold, job should fail")
    void when_errorCount_is_not_below_threshold_fail_job() {

        reset(mockCoverageDriver);
        when(mockCoverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(Contract.class))).thenReturn(40);
        andThenAnswerPatients(mockCoverageDriver, contractForCoverageDTO, 10, 40);

        OngoingStubbing<IBaseBundle> stubbing = when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong(), any()));
        stubbing = andThenAnswerEobs(stubbing, 0, 20);
        stubbing = stubbing.thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail);
        andThenAnswerEobs(stubbing, 30, 10);

        var processedJob = cut.process(job.getJobUuid());

        List<LoggableEvent> errorEvents = loggerEventRepository.load(ErrorEvent.class);
        assertEquals(1, errorEvents.size());
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(TOO_MANY_SEARCH_ERRORS, errorEvent.getErrorType());

        List<LoggableEvent> jobEvents = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, jobEvents.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals("IN_PROGRESS", jobEvent.getOldStatus());
        assertEquals("FAILED", jobEvent.getNewStatus());

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        // Since the max size of the file is not set here (so it's 0), every second write creates a new file since
        // the file is no longer empty after the first write. This means, there were 20 files created so 40 events
        assertEquals(40, fileEvents.size());
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.OPEN).count());
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.CLOSE).count());
        assertTrue(((FileEvent) fileEvents.get(39)).getFileName().contains("0020.ndjson"));
        assertTrue(((FileEvent) fileEvents.get(0)).getFileName().contains("0001.ndjson"));
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getFileHash().length() > 0).count());

        assertTrue(UtilMethods.allEmpty(loggerEventRepository.load(ApiRequestEvent.class),
                loggerEventRepository.load(ApiResponseEvent.class),
                loggerEventRepository.load(ReloadEvent.class)
        ));

        // Even though a job has failed, expect an event to be generated with the progress
        assertFalse(loggerEventRepository.load(ContractSearchEvent.class).isEmpty());

        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertEquals("Too many patient records in the job had failures", processedJob.getStatusMessage());
        assertNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        List<LoggableEvent> contractSearchEvents = loggerEventRepository.load(ContractSearchEvent.class);
        assertEquals(1, contractSearchEvents.size());
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Harry_Potter");
        pdpClient.setOrganization("Harry_Potter");
        pdpClient.setEnabled(TRUE);
        pdpClient =  pdpClientRepository.saveAndFlush(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private ContractWorkerDto createContract() {
        ContractWorkerDto contract = new ContractWorkerDto();
        contract.setContractName(CONTRACT_NAME);
        contract.setContractNumber(CONTRACT_NUMBER);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));

        contract = contractRepository.saveAndFlush(contract);
        dataSetup.queueForCleanup(contract);
        return contract;
    }

    private Job createJob(PdpClient pdpClient) {
        Job job = new Job();
        job.setJobUuid(JOB_UUID);
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setPdpClient(pdpClient);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        job.setContractNumber(contract.getContractNumber());

        job = jobRepository.saveAndFlush(job);
        dataSetup.queueForCleanup(job);
        return job;
    }

    private static List<CoverageSummary> loadFauxMetadata(ContractForCoverageDTO contract, int rowsToRetrieve) {

        List<Long> patientIdRows = LongStream.range(0, rowsToRetrieve).boxed().collect(toList());

        // Add the one id that actually has an eob mapped to it
        patientIdRows.add(-199900000022040L);

        return patientIdRows.stream().map(patientId -> new CoverageSummary(
                createIdentifierWithoutMbi(patientId),
                contract, List.of(getOpenRange())
        )).collect(toList());
    }

    private static OngoingStubbing<IBaseBundle> andThenAnswerEobs(OngoingStubbing<IBaseBundle> stubbing, int startId, int number) {

        for (int id = startId; id < startId + number; id++) {
            stubbing = stubbing.thenAnswer((args) -> {
                ExplanationOfBenefit copy = EOB.copy();
                copy.getPatient().setReference("Patient/" + args.getArgument(1));
                return EobTestDataUtil.createBundle(copy);
            });
        }

        return stubbing;
    }

    private static OngoingStubbing<CoveragePagingResult> andThenAnswerPatients(CoverageDriver coverageDriver, ContractForCoverageDTO contract, int pageSize, int total) {

        OngoingStubbing<CoveragePagingResult> stubbing = when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class)));

        List<CoverageSummary> fauxBeneficiaries = loadFauxMetadata(contract, total);
        for (int id = 0; id <= total - pageSize; id += pageSize) {
            stubbing = stubbing.thenReturn(new CoveragePagingResult(
                    fauxBeneficiaries.subList(id, id + pageSize),
                    new CoveragePagingRequest(pageSize, (long) id + pageSize, contract, OffsetDateTime.now())
            ));
        }

        return stubbing.thenReturn(new CoveragePagingResult(fauxBeneficiaries.subList(total - pageSize, total), null));
    }
}