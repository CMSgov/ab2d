package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.*;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.job.service.JobCleanup;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.util.HealthCheck;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
import static gov.cms.ab2d.fhir.FhirVersion.R4V3;
import static gov.cms.ab2d.worker.TestUtil.getOpenRange;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Adapted from {@link JobProcessorIntegrationTest} for v3
 */
@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
@ExtendWith({OutputCaptureExtension.class})
class JobProcessorV3IntegrationTest extends JobCleanup {

    private static final String CONTRACT_NAME = "CONTRACT_0001";
    private static final String CONTRACT_NUMBER = "CONTRACT_0001";
    private static final String JOB_UUID = "S0001";
    public static final String FINISHED_DIR = "finished";
    public static final String STREAMING_DIR = "streaming";
    public static final int NUMBER_PATIENT_REQUESTS_PER_THREAD = 5;
    public static final int MULTIPLIER = 2;

    private JobProcessor cut;       // class under test

    @Autowired
    private FileService fileService;

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
    private ContractServiceStub contractServiceStub;

    @Autowired
    private ContractWorkerClient contractWorkerClient;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Autowired
    private RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue;

    @Autowired
    private HealthCheck healthCheck;

    @Autowired
    private DataSetup dataSetup;

    @Mock
    private SQSEventClient sqsEventClient;

    @Autowired
    private ContractToContractCoverageMapping mapping;

    @Mock
    private CoverageDriver mockCoverageDriver;

    @Mock
    private BFDClient mockBfdClient;

    @Mock
    CoverageV3Service coverageV3Service;

    @Mock
    DataSource dataSource;

    @Autowired
    private PropertiesService propertiesService;

    @TempDir
    File tmpEfsMountDir;

    @Captor
    private ArgumentCaptor<LoggableEvent> captor;

    private Job job;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @DynamicPropertySource
    static void sqsProps(DynamicPropertyRegistry registry) {
        registry.add("AWS_SQS_URL", localstackContainer::getSqsEndpoint);
    }

    private static final ExplanationOfBenefit EOB = (ExplanationOfBenefit) EobTestDataUtil.createEOBV3();

    private Contract contract;
    private ContractForCoverageDTO contractForCoverageDTO;
    private RuntimeException fail;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PdpClient pdpClient = createClient();

        contract = createContract();
        contractForCoverageDTO = mapping.map(contract);
        fail = new RuntimeException("TEST EXCEPTION");

        job = createJob(pdpClient);
        job.setContractNumber(contract.getContractNumber());
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.saveAndFlush(job);

        when(mockBfdClient.requestEOBFromServer(eq(R4V3), anyLong(), anyString())).thenAnswer((args) -> {
            ExplanationOfBenefit copy = EOB.copy();
            copy.getPatient().setReference("Patient/" + args.getArgument(1));
            return EobTestDataUtil.createBundle(copy);
        });
        when(mockBfdClient.requestEOBFromServer(eq(R4V3), anyLong(), any(), any(), any(), anyString())).thenAnswer(args -> {
            ExplanationOfBenefit copy = EOB.copy();
            copy.getPatient().setReference("Patient/" + args.getArgument(1));
            return EobTestDataUtil.createBundle(copy);
        });

        when(mockCoverageDriver.numberOfBeneficiariesToProcessV3(any(Job.class), any(ContractDTO.class))).thenReturn(100);

        when(mockCoverageDriver.pageCoverageV3(any(CoveragePagingRequest.class))).thenReturn(
                new CoveragePagingResult(loadFauxMetadata(contractForCoverageDTO, 99), null));

        SearchConfig searchConfig = new SearchConfig(tmpEfsMountDir.getAbsolutePath(),
                STREAMING_DIR, FINISHED_DIR, 0, 0, MULTIPLIER, NUMBER_PATIENT_REQUESTS_PER_THREAD);

        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, sqsEventClient, searchConfig, propertiesService, dataSource);
        ReflectionTestUtils.setField(patientClaimsProcessor, "earliestDataDate", "01/01/1900");

        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.initialize();

        ContractProcessor contractProcessor = new ContractProcessorImpl(
                contractWorkerClient,
                jobRepository,
                mockCoverageDriver,
                patientClaimsProcessor,
                sqsEventClient,
                eobClaimRequestsQueue,
                jobChannelService,
                jobProgressService,
                mapping,
                pool,
                searchConfig);

        cut = new JobProcessorImpl(
                fileService,
                jobChannelService,
                jobProgressService,
                jobProgressUpdateService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                sqsEventClient,
                coverageV3Service
        );

        ReflectionTestUtils.setField(cut, "efsMount", tmpEfsMountDir.toString());
        ReflectionTestUtils.setField(cut, "failureThreshold", 10);
    }

    @AfterEach
    void cleanup() {
        jobCleanup();
        dataSetup.cleanup();
        pdpClientRepository.deleteAll();
    }

    @Test
    @DisplayName("v3 sync fails if max attempts exceeded (IDR import takes longer than expected)")
    void testV3SyncRetriesExceededIdrImporter(CapturedOutput out) {
        ((JobProcessorImpl)cut).setWaitTimeIfIdrImporterInProgress(Duration.ofSeconds(1));
        when(coverageV3Service.moveFromStagingToRecentCoverage(any(), any())).thenReturn(IDR_IMPORTER_IN_PROGRESS);
        cut.process(job.getJobUuid());
        assertTrue(out.getOut().contains("moveFromStagingToRecentCoverage() returned IDR_IMPORTER_IN_PROGRESS; Waiting 1 seconds before retrying sync"));
        assertTrue(out.getOut().contains("Unexpected exception executing job trySyncCoverageV3 failed with IDR_IMPORTER_IN_PROGRESS after 3 attempts"));
        assertTrue(out.getOut().contains("Job: [S0001] FAILED"));
    }

    @Test
    @DisplayName("v3 sync fails if max attempts exceeded (unable to acquire lock)")
    void testV3SyncRetriesExceededGenericError(CapturedOutput out) {
        ((JobProcessorImpl)cut).setWaitTimeIfIdrImporterInProgress(Duration.ofSeconds(1));
        when(coverageV3Service.moveFromStagingToRecentCoverage(any(), any())).thenReturn(UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT);
        cut.process(job.getJobUuid());
        assertTrue(out.getOut().contains("moveFromStagingToRecentCoverage() returned UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT; Retrying sync"));
        assertTrue(out.getOut().contains("Unexpected exception executing job trySyncCoverageV3 failed with UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT after 3 attempts"));
        assertTrue(out.getOut().contains("Job: [S0001] FAILED"));
    }

    @ParameterizedTest
    @DisplayName("v3 sync successful")
    @EnumSource(
            value = CoverageV3SyncResult.class,
            names = {"SYNC_SUCCESSFUL_FOR_CONTRACT", "NO_COVERAGE_FOUND_FOR_CONTRACT"}
    )
    void testV3SyncSuccessful(CoverageV3SyncResult result, CapturedOutput out) {
        ((JobProcessorImpl)cut).setWaitTimeIfIdrImporterInProgress(Duration.ofSeconds(1));
        when(coverageV3Service.moveFromStagingToRecentCoverage(any(), any())).thenReturn(result);
        cut.process(job.getJobUuid());
        assertTrue(out.getOut().contains("moveFromStagingToRecentCoverage() completed with %s".formatted(result)));
        assertTrue(out.getOut().contains("Job: [S0001] is DONE"));
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Z0001");
        pdpClient.setOrganization("XYZ Corp");
        pdpClient.setEnabled(TRUE);
        pdpClient =  pdpClientRepository.saveAndFlush(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private Contract createContract() {
        Contract newContract = new Contract();
        newContract.setContractName(CONTRACT_NAME);
        newContract.setContractNumber(CONTRACT_NUMBER);
        newContract.setAttestedOn(OffsetDateTime.now().minusDays(10));

        contractServiceStub.updateContract(newContract);
        dataSetup.queueForCleanup(newContract);
        return newContract;
    }

    private Job createJob(PdpClient pdpClient) {
        Job newJob = new Job();
        newJob.setFhirVersion(R4V3);
        newJob.setJobUuid(JOB_UUID);
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setOrganization(pdpClient.getOrganization());
        newJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setContractNumber(contract.getContractNumber());

        newJob = jobRepository.saveAndFlush(newJob);
        addJobForCleanup(newJob);
        return newJob;
    }

    private static List<CoverageSummary> loadFauxMetadata(ContractForCoverageDTO contract, int rowsToRetrieve) {

        List<Long> patientIdRows = LongStream.range(0, rowsToRetrieve).boxed().collect(toList());

        // Add the one id that actually has an eob mapped to it
        patientIdRows.add(-199900000022040L);

        return patientIdRows.stream().map(patientId -> new CoverageSummary(
                createIdentifierWithoutMbi(patientId),
                contract, List.of(getOpenRange())
        )).toList();
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
