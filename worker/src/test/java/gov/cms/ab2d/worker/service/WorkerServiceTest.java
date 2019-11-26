package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.ClassRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


/**
 * Generic Tests to make sure that the worker gets triggered upon submitting a job into the Job table.
 */
@SpringBootTest
public class WorkerServiceTest {
    private final Random random = new Random();

    @Autowired private JobRepository jobRepository;
    @Autowired private SponsorRepository sponsorRepository;
    @Autowired private UserRepository userRepository;

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = AB2DPostgresqlContainer.getInstance();

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

        final User user = createUser();
        Job submittedJob1 = createJob(user);
        Job submittedJob2 = createJob(user);

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
        job.setResourceTypes("ExplanationOfBenefits");
        job.setCreatedAt(OffsetDateTime.now());
        job.setUser(user);
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


    private void checkResult(Job processedJob) {
        assertThat(processedJob.getStatus(), equalTo(SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), equalTo("100%"));
    }

}