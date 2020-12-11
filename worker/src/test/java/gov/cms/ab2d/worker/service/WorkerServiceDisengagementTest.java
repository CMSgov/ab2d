package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static gov.cms.ab2d.common.model.JobStatus.*;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Generic Tests to make sure that the worker gets triggered upon submitting a job into the Job table.
 */
@SpringBootTest
@Testcontainers
class WorkerServiceDisengagementTest {
    private final Random random = new Random();

    @Autowired private DataSetup dataSetup;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ContractRepository contractRepository;
    @Autowired private PropertiesService propertiesService;

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void init() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        dataSetup.deleteCoverage();
        contractRepository.deleteAll();
        disableWorker();
    }

    @AfterEach
    public void cleanup() {
        enableWorker();
    }

    private void disableWorker() {
        setEngagement(FeatureEngagement.NEUTRAL);
    }

    private void enableWorker() {
        setEngagement(FeatureEngagement.IN_GEAR);
    }

    private void setEngagement(FeatureEngagement drive) {
        List<PropertiesDTO> propertiesDTOS = propertiesService.getAllPropertiesDTO();
        Optional<PropertiesDTO> dto = propertiesDTOS.stream()
                .filter(tmpDto -> tmpDto.getKey().equals(Constants.WORKER_ENGAGEMENT))
                .findAny();
        if (dto.isEmpty()) {
            throw new IllegalStateException(Constants.WORKER_ENGAGEMENT + " must be set.");
        }
        dto.get().setValue(drive.getSerialValue());
        propertiesService.updateProperties(propertiesDTOS);
    }

    @Test
    @DisplayName("When a job is submitted into the job table, a disengaged worker never processes it")
    void whenJobSubmittedWorkerGetsTriggered() throws InterruptedException {

        final User user = createUser();
        Job submittedJob = createJob(user);

        Thread.sleep(6000L);

        Job processedJob = jobRepository.findByJobUuid(submittedJob.getJobUuid());
        checkIdleResult(processedJob);

        // Now confirm that switching workers back on ... works!
        enableWorker();
        Thread.sleep(6000L);
        processedJob = jobRepository.findByJobUuid(submittedJob.getJobUuid());
        checkEngagedResult(processedJob);
    }

    @Test
    @DisplayName("When multiple jobs are submitted into the job table, they are processed in parallel by the workers")
    void whenTwoJobsSubmittedWorkerGetsTriggeredProcessesBothInParallel() throws InterruptedException {

        Job submittedJob1 = createJob(createUser());
        Job submittedJob2 = createJob(createUser2());

        // There is a 5 second sleep in the WorkerService.
        // So if the result for two jobs comes before 10 seconds, it implies they were not processed sequentially
        Thread.sleep(10000L);

        Job processedJob1 = jobRepository.findByJobUuid(submittedJob1.getJobUuid());
        checkIdleResult(processedJob1);

        Job processedJob2 = jobRepository.findByJobUuid(submittedJob2.getJobUuid());
        checkIdleResult(processedJob2);

        // Now confirm that switching workers back on ... works!
        enableWorker();
        Thread.sleep(10000L);

        processedJob1 = jobRepository.findByJobUuid(submittedJob1.getJobUuid());
        checkEngagedResult(processedJob1);

        processedJob2 = jobRepository.findByJobUuid(submittedJob2.getJobUuid());
        checkEngagedResult(processedJob2);
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
        return jobRepository.save(job);
    }

    private User createUser() {
        final User user = new User();
        user.setId((long) getIntRandom());
        user.setUsername("testuser" + getIntRandom());
        user.setEnabled(true);
        user.setContract(dataSetup.setupContract("W1234"));
        return userRepository.save(user);
    }

    private User createUser2() {
        final User user = new User();
        user.setId((long) getIntRandom());
        user.setUsername("testuser2" + getIntRandom());
        user.setEnabled(true);
        user.setContract(dataSetup.setupContract("W5678"));
        return userRepository.save(user);
    }

    private int getIntRandom() {
        return random.nextInt(100);
    }


    private void checkIdleResult(Job processedJob) {
        assertEquals(SUBMITTED, processedJob.getStatus());
        assertEquals( "0%", processedJob.getStatusMessage());
    }

    private void checkEngagedResult(Job processedJob) {
        assertEquals(processedJob.getStatus(), SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");    }
}