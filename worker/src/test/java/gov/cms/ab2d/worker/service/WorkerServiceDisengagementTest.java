package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.service.WorkerDrive;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
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

import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static org.junit.Assert.assertEquals;


/**
 * Generic Tests to make sure that the worker gets triggered upon submitting a job into the Job table.
 */
@SpringBootTest
@Testcontainers
public class WorkerServiceDisengagementTest {
    private final Random random = new Random();

    @Autowired private JobRepository jobRepository;
    @Autowired private SponsorRepository sponsorRepository;
    @Autowired private CoverageRepository coverageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertiesService propertiesService;

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void init() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        coverageRepository.deleteAll();
        sponsorRepository.deleteAll();
        disableWorker();
    }

    @AfterEach
    public void cleanup() {
        enableWorker();
    }

    private void disableWorker() {
        setEngagement(WorkerDrive.NEUTRAL);
    }

    private void enableWorker() {
        setEngagement(WorkerDrive.IN_GEAR);
    }

    private void setEngagement(WorkerDrive drive) {
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

        final User user = createUser();
        Job submittedJob1 = createJob(user);
        Job submittedJob2 = createJob(user);

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
        user.setSponsor(createSponsor());
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Sponsor createSponsor() {
        final Sponsor parentSponsor = new Sponsor();
        parentSponsor.setId((long) getIntRandom());
        parentSponsor.setHpmsId(getIntRandom());
        parentSponsor.setOrgName("BCBS - PARENT");
        sponsorRepository.save(parentSponsor);

        final Sponsor sponsor = new Sponsor();
        sponsor.setId((long) getIntRandom());
        sponsor.setHpmsId(getIntRandom());
        sponsor.setOrgName("BCBS");
        sponsor.setParent(parentSponsor);
        return sponsorRepository.save(sponsor);
    }

    private int getIntRandom() {
        return random.nextInt(100);
    }


    private void checkIdleResult(Job processedJob) {
        assertEquals(processedJob.getStatus(), SUBMITTED);
        assertEquals(processedJob.getStatusMessage(), "0%");
    }

    private void checkEngagedResult(Job processedJob) {
        assertEquals(processedJob.getStatus(), SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
    }

}