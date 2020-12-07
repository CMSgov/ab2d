package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.*;
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
import java.util.Random;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class JobPreProcessorIntegrationTest {
    private Random random = new Random();

    private JobPreProcessor cut;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DoAll doAll;
    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    private User user;
    private Job job;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        LogManager manager = new LogManager(sqlEventLogger, kinesisEventLogger);

        cut = new JobPreProcessorImpl(jobRepository, manager);

        user = createUser();
        job = createJob(user);
    }

    @AfterEach
    void clear() {

        doAll.delete();

        jobRepository.deleteAll();

        if (user != null) {
            userRepository.delete(user);
            userRepository.flush();
        }
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() {
        var processedJob = cut.preprocess("S0000");
        assertEquals(processedJob.getStatus(), JobStatus.IN_PROGRESS);

        List<LoggableEvent> jobStatusChange = doAll.load(JobStatusChangeEvent.class);
        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals("SUBMITTED", event.getOldStatus());
        assertEquals("IN_PROGRESS", event.getNewStatus());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class)));
        doAll.delete();
    }

    @Test
    @DisplayName("Throw an exception if the job does not exist")
    void putNonExistentJobInProgress() {
        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess("NonExistent"));
        assertThat(exceptionThrown.getMessage(), is("Job NonExistent was not found"));
    }

    @Test
    @DisplayName("When a job is not already in a submitted status, it cannot be put into progress")
    void whenJobIsNotInSubmittedStatus_ThenJobShouldNotBePutInProgress() {
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess("S0000"));

        assertThat(exceptionThrown.getMessage(), is("Job S0000 is not in SUBMITTED status"));
    }

    private User createUser() {
        User user = new User();
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.edu");
        user.setEnabled(TRUE);
        user.setContract(new Contract());
        return userRepository.save(user);
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setUser(user);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }
}