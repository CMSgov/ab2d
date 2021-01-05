package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.util.HealthCheck;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.transaction.Transactional;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS;
import static gov.cms.ab2d.worker.TestUtil.getOpenRange;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
@Transactional
class JobProcessorIntegrationTest {

    private static final String CONTRACT_NAME = "CONTRACT_0000";
    private static final String CONTRACT_NUMBER = "CONTRACT_0000";
    private static final String JOB_UUID = "S0000";
    public static final String COMPLETED_PERCENT = "100%";

    private JobProcessor cut;       // class under test

    @Autowired
    private FileService fileService;

    @Autowired
    private DoAll doAll;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Autowired
    private HealthCheck healthCheck;

    @Mock
    private CoverageDriver coverageDriver;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    @Mock
    private BFDClient mockBfdClient;

    @TempDir
    File tmpEfsMountDir;

    private Job job;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();
    private Bundle bundle1;
    private Bundle[] bundles;
    private RuntimeException fail;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        doAll.delete();

        LogManager logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        User user = createUser();

        Contract contract = createContract();
        contract = contractRepository.saveAndFlush(contract);

        job = createJob(user);
        job.setContract(contract);
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.saveAndFlush(job);

        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundles = getBundles();
        when(mockBfdClient.requestEOBFromServer(anyString())).thenReturn(bundle1);
        when(mockBfdClient.requestEOBFromServer(anyString(), any())).thenReturn(bundle1);

        fail = new RuntimeException("TEST EXCEPTION");

        FhirContext fhirContext = FhirContext.forDstu3();
        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, logManager);
        ReflectionTestUtils.setField(patientClaimsProcessor, "startDate", "01/01/1900");
        ContractProcessor contractProcessor = new ContractProcessorImpl(
                fileService,
                jobRepository,
                patientClaimsProcessor,
                logManager,
                fhirContext
        );

        ReflectionTestUtils.setField(contractProcessor, "cancellationCheckFrequency", 10);

        coverageDriver = mock(CoverageDriver.class);

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class))).thenReturn(100);

        when(coverageDriver.pageCoverage(any(Job.class))).thenReturn(
                new CoveragePagingResult(loadFauxMetadata(contract, 99), null));

        cut = new JobProcessorImpl(
                fileService,
                jobRepository,
                jobOutputRepository,
                contractProcessor,
                coverageDriver,
                logManager
        );

        ReflectionTestUtils.setField(cut, "efsMount", tmpEfsMountDir.toString());
        ReflectionTestUtils.setField(cut, "failureThreshold", 10);
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob() {
        var processedJob = cut.process(job.getJobUuid());

        List<LoggableEvent> jobStatusChange = doAll.load(JobStatusChangeEvent.class);
        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals(JobStatus.SUCCESSFUL.name(), jobEvent.getNewStatus());
        assertEquals(JobStatus.IN_PROGRESS.name(), jobEvent.getOldStatus());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    @DisplayName("When a job is in submitted by the parent user, it process the contracts for the children")
    void whenJobSubmittedByParentUser_ProcessAllContractsForChildrenSponsors() {
        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    @DisplayName("When the error count is below threshold, job does not fail")
    void when_errorCount_is_below_threshold_do_not_fail_job() {
        when(mockBfdClient.requestEOBFromServer(anyString()))
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundle1, bundle1, bundle1, bundle1)
                .thenThrow(fail, fail, fail, fail, fail);

        var processedJob = cut.process(job.getJobUuid());

        assertEquals(JobStatus.SUCCESSFUL, processedJob.getStatus());
        assertEquals(COMPLETED_PERCENT, processedJob.getStatusMessage());
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        List<LoggableEvent> beneSearchEvents = doAll.load(ContractBeneSearchEvent.class);
        assertEquals(1, beneSearchEvents.size());
        ContractBeneSearchEvent event = (ContractBeneSearchEvent) beneSearchEvents.get(0);
        assertEquals(JOB_UUID, event.getJobId());
        assertEquals(100, event.getNumInContract());
        assertEquals(CONTRACT_NAME, event.getContractNumber());
        assertEquals(100, event.getNumSearched());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    void testHealth() {
        assertTrue(healthCheck.healthy());
    }

    @Test
    @DisplayName("When the error count is greater than or equal to threshold, job should fail")
    void when_errorCount_is_not_below_threshold_fail_job() {
        when(mockBfdClient.requestEOBFromServer(anyString(), any()))
                .thenReturn(bundle1, bundles)
                .thenReturn(bundle1, bundles)
                .thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail)
                .thenReturn(bundle1, bundles)
        ;
        var processedJob = cut.process(job.getJobUuid());

        List<LoggableEvent> errorEvents = doAll.load(ErrorEvent.class);
        assertEquals(1, errorEvents.size());
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(TOO_MANY_SEARCH_ERRORS, errorEvent.getErrorType());

        List<LoggableEvent> jobEvents = doAll.load(JobStatusChangeEvent.class);
        assertEquals(1, jobEvents.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals("IN_PROGRESS", jobEvent.getOldStatus());
        assertEquals("FAILED", jobEvent.getNewStatus());

        List<LoggableEvent> fileEvents = doAll.load(FileEvent.class);
        // Since the max size of the file is not set here (so it's 0), every second write creates a new file since
        // the file is no longer empty after the first write. This means, there were 20 files created so 40 events
        assertEquals(40, fileEvents.size());
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.OPEN).count());
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.CLOSE).count());
        assertTrue(((FileEvent) fileEvents.get(39)).getFileName().contains("0020.ndjson"));
        assertTrue(((FileEvent) fileEvents.get(0)).getFileName().contains("0001.ndjson"));
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getFileHash().length() > 0).count());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class)));

        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertEquals(processedJob.getStatusMessage(), "Too many patient records in the job had failures");
        assertNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());
    }

    private Bundle[] getBundles() {
        return new Bundle[]{bundle1, bundle1, bundle1, bundle1, bundle1, bundle1, bundle1, bundle1, bundle1};
    }

    private User createUser() {
        User user = new User();
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.com");
        user.setEnabled(TRUE);
//        user.setContract(createContract());
        return userRepository.save(user);
    }

    private Contract createContract() {
        Contract contract = new Contract();
        contract.setContractName(CONTRACT_NAME);
        contract.setContractNumber(CONTRACT_NUMBER);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));

        return contractRepository.save(contract);
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid(JOB_UUID);
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setUser(user);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }

    private static List<CoverageSummary> loadFauxMetadata(Contract contract, int rowsToRetrieve) {

        List<String> patientIdRows = IntStream.range(0, rowsToRetrieve).mapToObj(i -> "-" + i).collect(toList());

        // Add the one id that actually has an eob mapped to it
        patientIdRows.add("-199900000022040");

        return patientIdRows.stream().map(patientId -> new CoverageSummary(
                createIdentifierWithoutMbi(patientId),
                contract, List.of(getOpenRange())
        )).collect(toList());
    }
}