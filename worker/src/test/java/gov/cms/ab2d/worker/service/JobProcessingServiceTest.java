package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Random;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class JobProcessingServiceTest {
    private Random random = new Random();

    @Autowired
    private JobProcessingService jobProcessingService;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private SponsorRepository sponsorRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContractRepository contractRepository;

    private Sponsor sponsor;
    private User user;
    private Job job;

    @Container
    public static PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();

        sponsor = createSponsor();
        user = createUser(sponsor);
        job = createJob(user);
    }


    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() {
        var processedJob = jobProcessingService.putJobInProgress("S001");
        assertThat(processedJob.getStatus(), is(JobStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void putNonExistentJobInProgress() {
        var exceptionThrown = assertThrows(IllegalArgumentException.class,() ->
                jobProcessingService.putJobInProgress("NonExistent"));
        assertThat(exceptionThrown.getMessage(), is("Job NonExistent was not found"));
    }

    @Test
    @DisplayName("When a job is not already in a submitted status, it cannot be put into progress")
    void whenJobIsNotInSubmittedStatus_ThenJobShouldNotBePutInProgress() {
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        var exceptionThrown = assertThrows(IllegalArgumentException.class,
                () -> jobProcessingService.putJobInProgress("S001"));

        assertThat(exceptionThrown.getMessage(), is("Job S001 is not in SUBMITTED status."));
    }


    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob() {

        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);
        createContract(sponsor);

        var processedJob = jobProcessingService.processJob("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        assertThat(processedJob.getCompletedAt(), notNullValue());
    }

    @Test
    @DisplayName("When a job is in submitted by the parent user, it process the contracts for the children")
    void whenJobSubmittedByParentUser_ProcessAllContractsForChildrenSponsors() throws IOException {

        // create parent sponsor
        final Sponsor parentSponsor = createSponsor();
        parentSponsor.setOrgName(parentSponsor.getOrgName() + " - PARENT");
        parentSponsor.setLegalName(parentSponsor.getLegalName() + " - PARENT");

        // associate the parent to the child
        final Sponsor childSponsor = user.getSponsor();
        childSponsor.setParent(parentSponsor);
        sponsorRepository.save(childSponsor);

        // switch the user to the parent sponsor
        user.setSponsor(parentSponsor);
        userRepository.save(user);

        createContract(sponsor);

        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        var processedJob = jobProcessingService.processJob("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        assertThat(processedJob.getCompletedAt(), notNullValue());
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

    private Contract createContract(Sponsor sponsor) {
        final int anInt = random.nextInt(299);
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_0000" + anInt);
        contract.setContractNumber("CONTRACT_0000" + anInt);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return contractRepository.save(contract);
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid("S001");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setUser(user);
        job.setCreatedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }
}