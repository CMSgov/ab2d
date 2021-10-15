package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.util.HealthCheck;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.TestUtil.getOpenRange;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
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

    @Mock
    private CoverageDriver coverageDriver;

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
    private RuntimeException fail;

    @BeforeEach
    void setUp() {
        LogManager logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger);
        PdpClient pdpClient = createClient();

        Contract contract = createContract();

        job = createJob(pdpClient);
        job.setContract(contract);
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

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class))).thenReturn(100);

        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class))).thenReturn(
                new CoveragePagingResult(loadFauxMetadata(contract, 99), null));


        fail = new RuntimeException("TEST EXCEPTION");

        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, logManager);
        ReflectionTestUtils.setField(patientClaimsProcessor, "earliestDataDate", "01/01/1900");
        ContractProcessor contractProcessor = new ContractProcessorImpl(
                jobRepository,
                coverageDriver,
                patientClaimsProcessor,
                logManager,
                eobClaimRequestsQueue,
                jobChannelService,
                jobProgressService);


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
    @DisplayName("When the error count is below threshold, job does not fail")
    void when_errorCount_is_below_threshold_do_not_fail_job() {
        OngoingStubbing<IBaseBundle> stubbing = when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong()));
        stubbing = addThenAnswer(stubbing, 0, 95);
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
        assertEquals(CONTRACT_NAME, event.getContractNumber());
        assertEquals(100, event.getBenesSearched());

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
        OngoingStubbing<IBaseBundle> stubbing = when(mockBfdClient.requestEOBFromServer(eq(STU3), anyLong(), any()));
        stubbing = addThenAnswer(stubbing, 0, 20);
        stubbing = stubbing.thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail);
        addThenAnswer(stubbing, 30, 10);

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

    private Contract createContract() {
        Contract contract = new Contract();
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

        job = jobRepository.saveAndFlush(job);
        dataSetup.queueForCleanup(job);
        return job;
    }

    private static List<CoverageSummary> loadFauxMetadata(Contract contract, int rowsToRetrieve) {

        List<Long> patientIdRows = LongStream.range(0, rowsToRetrieve).mapToObj(obj -> obj).collect(toList());

        // Add the one id that actually has an eob mapped to it
        patientIdRows.add(-199900000022040L);

        return patientIdRows.stream().map(patientId -> new CoverageSummary(
                createIdentifierWithoutMbi(patientId),
                contract, List.of(getOpenRange())
        )).collect(toList());
    }

    private static OngoingStubbing<IBaseBundle> addThenAnswer(OngoingStubbing<IBaseBundle> stubbing, int startId, int number) {

        for (int id = startId; id < startId + number; id++) {
            stubbing = stubbing.thenAnswer((args) -> {
                ExplanationOfBenefit copy = EOB.copy();
                copy.getPatient().setReference("Patient/" + args.getArgument(1));
                return EobTestDataUtil.createBundle(copy);
            });
        }

        return stubbing;
    }
}