package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.PatientClaimsProcessor;
import gov.cms.ab2d.worker.adapter.bluebutton.PatientClaimsProcessorImpl;
import gov.cms.ab2d.worker.service.FileService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.transaction.Transactional;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.Random;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
@Transactional
class JobProcessorIntegrationTest {
    private Random random = new Random();

    private JobProcessor cut;       // class under test

    @Autowired
    private FileService fileService;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private SponsorRepository sponsorRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private JobOutputRepository jobOutputRepository;
    @Autowired
    private BeneficiaryAdapter beneficiaryAdapter;
    @Autowired
    private OptOutRepository optOutRepository;

    @Mock
    private BFDClient mockBfdClient;

    @TempDir
    File tmpEfsMountDir;

    private Sponsor sponsor;
    private User user;
    private Job job;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();

        sponsor = createSponsor();
        user = createUser(sponsor);
        job = createJob(user);

        Bundle bundle1 = new Bundle();
        when(mockBfdClient.requestEOBFromServer(anyString())).thenReturn(bundle1);

        FhirContext fhirContext = new FhirContext();
        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, fhirContext, fileService);

        cut = new JobProcessorImpl(fileService, jobRepository, jobOutputRepository, beneficiaryAdapter, patientClaimsProcessor,
                optOutRepository);
        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 10);
        ReflectionTestUtils.setField(cut, "efsMount", tmpEfsMountDir.toString());
    }


    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob() {
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);
        createContract(sponsor);

        var processedJob = cut.process("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        assertThat(processedJob.getCompletedAt(), notNullValue());
    }

    @Test
    @DisplayName("When a job is in submitted by the parent user, it process the contracts for the children")
    void whenJobSubmittedByParentUser_ProcessAllContractsForChildrenSponsors() {

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

        var processedJob = cut.process("S001");

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