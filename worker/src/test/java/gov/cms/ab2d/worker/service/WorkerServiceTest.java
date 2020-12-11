package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Generic Tests to make sure that the worker gets triggered upon submitting a job into the Job table.
 */
@SpringBootTest
@Testcontainers
class WorkerServiceTest {
    private final Random random = new Random();

    @Autowired private DataSetup dataSetup;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void init() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        dataSetup.deleteCoverage();
        contractRepository.deleteAll();
    }

    @Test
    @DisplayName("When a job is submitted into the job table, a worker processes it")
    void whenJobSubmittedWorkerGetsTriggered() throws InterruptedException {

        final User user = createUser();
        Job submittedJob = createJob(user);

        Thread.sleep(6000L);

        final Job processedJob = jobRepository.findByJobUuid(submittedJob.getJobUuid());
        checkResult(processedJob);
    }

    @Test
    @DisplayName("When multiple jobs are submitted into the job table, they are processed in parallel by the workers")
    void whenTwoJobsSubmittedWorkerGetsTriggeredProcessesBothInParallel() throws InterruptedException {

        Job submittedJob1 = createJob(createUser());
        Job submittedJob2 = createJob(createUser2());

        // There is a 5 second sleep in the WorkerService.
        // So if the result for two jobs comes before 10 seconds, it implies they were not processed sequentially
        Thread.sleep(10000L);

        final Job processedJob1 = jobRepository.findByJobUuid(submittedJob1.getJobUuid());
        checkResult(processedJob1);

        final Job processedJob2 = jobRepository.findByJobUuid(submittedJob2.getJobUuid());
        checkResult(processedJob2);
    }

    private Job createJob(final User user) {
        Job job = new Job();
        job.setId((long) getIntRandom());
        job.setJobUuid(UUID.randomUUID().toString());
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setResourceTypes(EOB);
        job.setCreatedAt(OffsetDateTime.now());
        job.setUser(user);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setContract(user.getContract());
        return jobRepository.save(job);
    }

    private User createUser() {
        final User user = new User();
        user.setId((long) getIntRandom());
        user.setUsername("testuser" + getIntRandom());
        user.setEnabled(true);
        user.setContract(dataSetup.setupContract("W9876"));
        return userRepository.save(user);
    }

    private User createUser2() {
        final User user = new User();
        user.setId((long) getIntRandom());
        user.setUsername("testuser2" + getIntRandom());
        user.setEnabled(true);
        user.setContract(dataSetup.setupContract("W8765"));
        return userRepository.save(user);
    }

    private int getIntRandom() {
        return random.nextInt(100);
    }


    private void checkResult(Job processedJob) {
        assertEquals(SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());    }
}