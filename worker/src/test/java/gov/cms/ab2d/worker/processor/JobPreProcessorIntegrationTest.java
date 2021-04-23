package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.*;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class JobPreProcessorIntegrationTest {

    private JobPreProcessor cut;

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

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        LogManager manager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger);

        cut = new JobPreProcessorImpl(jobRepository, manager, coverageDriver);

        pdpClient = createClient();
        job = createJob(pdpClient);
    }

    @AfterEach
    void clear() {

        loggerEventRepository.delete();
        dataSetup.cleanup();
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class))).thenReturn(true);

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
                loggerEventRepository.load(ContractBeneSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(FileEvent.class)));
        loggerEventRepository.delete();
    }

    @Test
    @DisplayName("When a job is not already in a submitted status, it cannot be put into progress")
    void whenJobIsNotInSubmittedStatus_ThenJobShouldNotBePutInProgress() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class))).thenReturn(true);

        job.setStatus(JobStatus.IN_PROGRESS);

        Job inProgress = jobRepository.save(job);

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess(inProgress.getJobUuid()));

        assertEquals("Job S0000 is not in SUBMITTED status", exceptionThrown.getMessage());
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

    private Job createJob(PdpClient pdpClient) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setPdpClient(pdpClient);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);

        job = jobRepository.save(job);
        dataSetup.queueForCleanup(job);
        return job;
    }
}