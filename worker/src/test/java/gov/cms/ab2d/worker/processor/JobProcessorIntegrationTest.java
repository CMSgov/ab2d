package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractAdapter;
import gov.cms.ab2d.worker.service.FileService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Resource;
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
    private ContractAdapter contractAdapter;
    @Autowired
    private OptOutRepository optOutRepository;

    @Mock
    private BFDClient mockBfdClient;

    @TempDir
    File tmpEfsMountDir;

    private Sponsor sponsor;
    private User user;
    private Job job;
    private ExplanationOfBenefit eob;

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

        createEOB();
        Bundle bundle1 = createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(anyString())).thenReturn(bundle1);

        FhirContext fhirContext = new FhirContext();
        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, fhirContext);

        cut = new JobProcessorImpl(fileService, jobRepository, jobOutputRepository, contractAdapter, patientClaimsProcessor,
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

        var processedJob = cut.process("S0000");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        assertThat(processedJob.getCompletedAt(), notNullValue());
    }

    @Test
    @DisplayName("When a job is in submitted by the parent user, it process the contracts for the children")
    void whenJobSubmittedByParentUser_ProcessAllContractsForChildrenSponsors() {
        // switch the user to the parent sponsor
        user.setSponsor(sponsor.getParent());
        userRepository.save(user);

        createContract(sponsor);

        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);

        var processedJob = cut.process("S0000");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        assertThat(processedJob.getCompletedAt(), notNullValue());
    }

    private Sponsor createSponsor() {
        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent");
        parent.setLegalName("Parent");
        parent.setHpmsId(350);

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Hogwarts School of Wizardry");
        sponsor.setLegalName("Hogwarts School of Wizardry LLC");
        sponsor.setHpmsId(random.nextInt());
        sponsor.setParent(parent);
        parent.getChildren().add(sponsor);
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
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_0000");
        contract.setContractNumber("CONTRACT_0000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return contractRepository.save(contract);
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


    private void createEOB() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        final String testInputFile = "test-data/EOB-for-Carrier-Claims.json";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);

        final EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        final IParser parser = respType.newParser(FhirContext.forDstu3());
        final ExplanationOfBenefit explanationOfBenefit = parser.parseResource(ExplanationOfBenefit.class, inputStream);
        eob = ExplanationOfBenefitTrimmer.getBenefit(explanationOfBenefit);
        Period billingPeriod = new Period();
        try {
            billingPeriod.setStart(sdf.parse("01/02/2020"));
            final LocalDate now = LocalDate.now();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            final String nowFormatted = now.format(formatter);
            billingPeriod.setEnd(sdf.parse(nowFormatted));
        } catch (Exception ex) {}
        eob.setBillablePeriod(billingPeriod);
    }

    private Bundle createBundle(Resource resource) {
        final Bundle bundle = new Bundle();
        bundle.addEntry(addEntry(resource));
        return bundle;
    }

    private Bundle.BundleEntryComponent addEntry(Resource resource) {
        final Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(resource);
        return bundleEntryComponent;
    }

}