package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
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
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static gov.cms.ab2d.common.model.JobStartedBy.DEVELOPER;
import static gov.cms.ab2d.common.model.SinceSource.AB2D;
import static gov.cms.ab2d.common.model.SinceSource.FIRST_RUN;
import static gov.cms.ab2d.common.model.SinceSource.USER;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class JobPreProcessorIntegrationTest {

    private JobPreProcessor cut;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Autowired
    private DataSetup dataSetup;

    @Mock
    private CoverageDriver coverageDriver;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    @Mock
    private SlackLogger slackLogger;

    private PdpClient pdpClient;
    private Job job;
    private Contract contract;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        LogManager manager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger);

        cut = new JobPreProcessorImpl(contractRepository, jobRepository, manager, coverageDriver);

        Contract tmpContract = new Contract();
        tmpContract.setContractNumber("JPP1234");
        tmpContract.setContractName(tmpContract.getContractNumber());
        contract = contractRepository.save(tmpContract);
        dataSetup.queueForCleanup(contract);
        pdpClient = createClient();
        job = createJob(pdpClient, contract.getContractNumber());
    }

    @AfterEach
    void clear() {

        loggerEventRepository.delete();
        dataSetup.cleanup();
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(Contract.class))).thenReturn(true);

        var processedJob = cut.preprocess(job.getJobUuid());
        assertEquals(JobStatus.IN_PROGRESS, processedJob.getStatus());

        List<LoggableEvent> jobStatusChange = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals("SUBMITTED", event.getOldStatus());
        assertEquals("IN_PROGRESS", event.getNewStatus());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ApiRequestEvent.class),
                loggerEventRepository.load(ApiResponseEvent.class),
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class)));
        loggerEventRepository.delete();
    }

    @Test
    @DisplayName("When a job is not already in a submitted status, it cannot be put into progress")
    void whenJobIsNotInSubmittedStatus_ThenJobShouldNotBePutInProgress() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(Contract.class))).thenReturn(true);

        job.setStatus(JobStatus.IN_PROGRESS);

        Job inProgress = jobRepository.save(job);

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess(inProgress.getJobUuid()));

        assertEquals("Job S0000 is not in SUBMITTED status", exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("When coverage fails, a job should fail")
    void whenCoverageFails_ThenJobShouldFail() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(Contract.class))).thenThrow(new CoverageDriverException("test"));

        var processedJob = cut.preprocess(job.getJobUuid());
        assertEquals(JobStatus.FAILED, processedJob.getStatus());

        List<LoggableEvent> jobStatusChange = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals("SUBMITTED", event.getOldStatus());
        assertEquals("FAILED", event.getNewStatus());

        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ApiRequestEvent.class),
                loggerEventRepository.load(ApiResponseEvent.class),
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class)));
        loggerEventRepository.delete();
    }

    @Test
    @DisplayName("We're R4, there have been successful jobs so set default since")
    void testDefaultSince() {
        // This is the last successful job run
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setStatusMessage("100%");
        oldJob.setPdpClient(pdpClient);
        oldJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        // This is an even early job (want to make sure it picks the correct old job)
        Job reallyOldJob = new Job();
        reallyOldJob.setJobUuid("CC-DD");
        reallyOldJob.setStatus(JobStatus.SUCCESSFUL);
        reallyOldJob.setStatusMessage("100%");
        reallyOldJob.setPdpClient(pdpClient);
        reallyOldJob.setStartedBy(DEVELOPER);
        reallyOldJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime reallyOldldJobTime = OffsetDateTime.parse("2020-12-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        reallyOldJob.setCreatedAt(reallyOldldJobTime);
        reallyOldJob.setFhirVersion(R4);
        reallyOldJob.setContractNumber(contract.getContractNumber());
        reallyOldJob = jobRepository.save(reallyOldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setPdpClient(pdpClient);
        newJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setFhirVersion(R4);
        newJob.setContractNumber(contract.getContractNumber());
        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertEquals(oldJobTime.getNano(), processedJob.getSince().getNano());
        assertEquals(AB2D, processedJob.getSinceSource());

        dataSetup.queueForCleanup(oldJob);
        dataSetup.queueForCleanup(reallyOldJob);
        dataSetup.queueForCleanup(newJob);
    }

    @Test
    @DisplayName("We're R4, there has been a failed job so don't set default since")
    void testDefaultSinceFailed() {
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.FAILED);
        oldJob.setStatusMessage("100%");
        oldJob.setPdpClient(pdpClient);
        oldJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setPdpClient(pdpClient);
        newJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime newJobTime = OffsetDateTime.parse("2021-02-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        newJob.setCreatedAt(newJobTime);
        newJob.setFhirVersion(R4);
        newJob.setContractNumber(contract.getContractNumber());
        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertNull(processedJob.getSince());
        assertEquals(FIRST_RUN, processedJob.getSinceSource());

        dataSetup.queueForCleanup(oldJob);
        dataSetup.queueForCleanup(newJob);
    }

    @Test
    @DisplayName("We're R4, there has been a successful job run but it was run by the developers so don't set default since")
    void testDefaultSinceAB2DRun() {
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setStatusMessage("100%");
        oldJob.setPdpClient(pdpClient);
        oldJob.setStartedBy(DEVELOPER);
        oldJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setPdpClient(pdpClient);
        newJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setFhirVersion(R4);
        newJob.setContractNumber(contract.getContractNumber());

        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertNull(processedJob.getSince());
        assertEquals(FIRST_RUN, processedJob.getSinceSource());

        dataSetup.queueForCleanup(oldJob);
        dataSetup.queueForCleanup(newJob);
    }

    @Test
    @DisplayName("We're R4, there has been a successful job run but we've specified a since date so don't set default since")
    void testDefaultSinceNotNeeded() {
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setStatusMessage("100%");
        oldJob.setPdpClient(pdpClient);
        oldJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setPdpClient(pdpClient);
        newJob.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime suppliedSince = OffsetDateTime.parse("2021-02-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        newJob.setSince(suppliedSince);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setFhirVersion(R4);
        newJob.setSince(suppliedSince);
        newJob.setContractNumber(contract.getContractNumber());
        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertEquals(suppliedSince.getNano(), processedJob.getSince().getNano());
        assertEquals(USER, processedJob.getSinceSource());

        dataSetup.queueForCleanup(oldJob);
        dataSetup.queueForCleanup(newJob);
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Harry_Potter");
        pdpClient.setOrganization("Harry_Potter");
        pdpClient.setEnabled(true);

        pdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private Job createJob(PdpClient pdpClient, String contractNumber) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setPdpClient(pdpClient);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        job.setContractNumber(contractNumber);

        job = jobRepository.save(job);
        dataSetup.queueForCleanup(job);
        return job;
    }
}