package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
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

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class JobPreProcessorIntegrationTest {
    private Random random = new Random();

    @Autowired
    private JobPreProcessor cut;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private SponsorRepository sponsorRepository;
    @Autowired
    private UserRepository userRepository;

    private Job job;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();

        var sponsor = createSponsor();
        var user = createUser(sponsor);
        job = createJob(user);
    }


    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() {
        var processedJob = cut.preprocess("S0000");
        assertThat(processedJob.getStatus(), is(JobStatus.IN_PROGRESS));
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


    private Sponsor createSponsor() {
        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Hogwarts School of Wizardry");
        sponsor.setLegalName("Hogwarts School of Wizardry LLC");
        sponsor.setHpmsId(random.nextInt());
        return sponsorRepository.save(sponsor);
    }

    private User createUser(Sponsor sponsor) {
        User user = new User();
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.com");
        user.setEnabled(TRUE);
        user.setSponsor(sponsor);
        return userRepository.save(user);
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setUser(user);
        job.setCreatedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }
}