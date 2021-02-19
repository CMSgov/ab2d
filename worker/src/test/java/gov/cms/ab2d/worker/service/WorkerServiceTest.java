package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.worker.config.JobHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;
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
    @Autowired private JobService jobService;
    @Autowired private PropertiesService propertiesService;

    @Autowired private WorkerServiceImpl workerServiceImpl;
    @Autowired private JobHandler jobHandler;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private WorkerServiceStub workerServiceStub;

    @BeforeEach
    public void init() {
        workerServiceStub = new WorkerServiceStub(jobService, propertiesService);

        ReflectionTestUtils.setField(jobHandler, "workerService", workerServiceStub);
    }

    @AfterEach
    public void cleanup() {
        ReflectionTestUtils.setField(jobHandler, "workerService", workerServiceImpl);

        dataSetup.cleanup();
    }

    @Test
    @DisplayName("When a job is submitted into the job table, a worker processes it")
    void whenJobSubmittedWorkerGetsTriggered() throws InterruptedException {

        final User user = createUser();
        createJob(user);

        Thread.sleep(6000L);

        assertEquals(1, workerServiceStub.processingCalls);
    }

    @Test
    @DisplayName("When multiple jobs are submitted into the job table, they are processed in parallel by the workers")
    void whenTwoJobsSubmittedWorkerGetsTriggeredProcessesBothInParallel() throws InterruptedException {

        createJob(createUser());
        createJob(createUser2());

        // There is a 5 second sleep in the WorkerService.
        // So if the result for two jobs comes before 10 seconds, it implies they were not processed sequentially
        Thread.sleep(10000L);

        assertEquals(2, workerServiceStub.processingCalls);
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
        job.setFhirVersion(STU3);

        job = jobRepository.save(job);
        dataSetup.queueForCleanup(job);
        return job;
    }

    private User createUser() {
        User user = new User();
        user.setId((long) getIntRandom());
        user.setUsername("testuser" + getIntRandom());
        user.setEnabled(true);
        user.setContract(dataSetup.setupContract("W9876"));

        user = userRepository.save(user);
        dataSetup.queueForCleanup(user);
        return user;
    }

    private User createUser2() {
        User user = new User();
        user.setId((long) getIntRandom());
        user.setUsername("testuser2" + getIntRandom());
        user.setEnabled(true);
        user.setContract(dataSetup.setupContract("W8765"));

        user = userRepository.save(user);
        dataSetup.queueForCleanup(user);
        return user;
    }

    private int getIntRandom() {
        return random.nextInt(100);
    }


    private void checkResult(Job processedJob) {
        assertEquals(SUCCESSFUL, processedJob.getStatus());
        assertEquals("100%", processedJob.getStatusMessage());    }
}