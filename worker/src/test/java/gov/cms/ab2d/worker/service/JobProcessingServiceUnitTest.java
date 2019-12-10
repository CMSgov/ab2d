package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.ConsentRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.adapter.bluebutton.PatientClaimsProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.AsyncResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class JobProcessingServiceUnitTest {
    private Random random = new Random();

    @Mock FileService fileService;
    @Mock JobRepository jobRepository;
    @Mock ConsentRepository consentRepository;
    @Mock JobOutputRepository jobOutputRepository;
    @Mock BeneficiaryAdapter beneficiaryAdapter;
    @Mock PatientClaimsProcessor patientClaimsProcessor;

    @Container
    public static PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    private Sponsor sponsor;
    private User user;
    private Job job;

    private JobProcessingService cut;

    @BeforeEach
    void setUp() {
        cut = new JobProcessingServiceImpl(
                fileService,
                jobRepository,
                jobOutputRepository,
                beneficiaryAdapter,
                patientClaimsProcessor,
                consentRepository
        );

        sponsor = createSponsor();
        user = createUser(sponsor);
        job = createJob(user);
    }


    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() {
        job.setStatus(JobStatus.SUBMITTED);

        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);
        when(jobRepository.save(Mockito.any())).thenReturn(job);

        var processedJob = cut.putJobInProgress("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.IN_PROGRESS));
        verify(jobRepository).findByJobUuid(Mockito.any());
        verify(jobRepository).save(Mockito.any());
    }

    @Test
    @DisplayName("When a job is not already in a submitted status, it cannot be put into progress")
    void whenJobIsNotInSubmittedStatus_ThenJobShouldNotBePutInProgress() {
        job.setStatus(JobStatus.IN_PROGRESS);

        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var exceptionThrown = assertThrows(IllegalArgumentException.class,
                () -> cut.putJobInProgress("S001"));

        assertThat(exceptionThrown.getMessage(), is("Job S001 is not in SUBMITTED status."));
        verify(jobRepository).findByJobUuid(Mockito.any());
    }


    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob(@TempDir Path efsMountTmpDir) throws IOException {

        job.setStatus(JobStatus.IN_PROGRESS);

        // create parent sponsor
        final Sponsor parentSponsor = createSponsor();
        parentSponsor.setOrgName(parentSponsor.getOrgName() + " - PARENT");
        parentSponsor.setLegalName(parentSponsor.getLegalName() + " - PARENT");

        // associate the parent to the child
        final Sponsor childSponsor = user.getSponsor();
        childSponsor.setParent(parentSponsor);
        parentSponsor.getChildren().add(childSponsor);

        // switch the user to the parent sponsor
        user.setSponsor(parentSponsor);

        var contract = createContract(sponsor);
        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var patientsByContract = createPatientsByContractResponse(contract);
        Mockito.when(beneficiaryAdapter.getPatientsByContract(anyString())).thenReturn(patientsByContract);

        when(fileService.createDirectory(Mockito.any(Path.class))).thenReturn(efsMountTmpDir);
        when(fileService.createFile(Mockito.any(Path.class), anyString()))
                .thenReturn(efsMountTmpDir)
                .thenReturn(efsMountTmpDir);

        Future<Integer> futureResources = new AsyncResult(0);
        Mockito.when(patientClaimsProcessor.process(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(futureResources);

        var processedJob = cut.processJob("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());

        verify(fileService).createDirectory(Mockito.any());
        verify(beneficiaryAdapter).getPatientsByContract(anyString());
        verify(patientClaimsProcessor, atLeast(1)).process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("When patient has opted out, their record will be skipped.")
    void processJob_whenPatientHasOptedOut_ShouldSkipPatientRecord(@TempDir Path efsMountTmpDir) throws IOException {

        job.setStatus(JobStatus.IN_PROGRESS);

        // create parent sponsor
        final Sponsor parentSponsor = createSponsor();
        parentSponsor.setOrgName(parentSponsor.getOrgName() + " - PARENT");
        parentSponsor.setLegalName(parentSponsor.getLegalName() + " - PARENT");

        // associate the parent to the child
        final Sponsor childSponsor = user.getSponsor();
        childSponsor.setParent(parentSponsor);
        parentSponsor.getChildren().add(childSponsor);

        // switch the user to the parent sponsor
        user.setSponsor(parentSponsor);

        var contract = createContract(sponsor);
        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var patientsByContract = createPatientsByContractResponse(contract);
        when(beneficiaryAdapter.getPatientsByContract(anyString())).thenReturn(patientsByContract);

        when(fileService.createDirectory(Mockito.any(Path.class))).thenReturn(efsMountTmpDir);
        when(fileService.createFile(Mockito.any(Path.class), anyString()))
                .thenReturn(efsMountTmpDir)
                .thenReturn(efsMountTmpDir);

        final List<Consent> consents = getConsents(patientsByContract);
        when(consentRepository.findByHicn(anyString()))
                .thenReturn(Arrays.asList(consents.get(0)))
                .thenReturn(Arrays.asList(consents.get(1)))
                .thenReturn(Arrays.asList(consents.get(2)));

        var processedJob = cut.processJob("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());

        verify(fileService).createDirectory(Mockito.any());
        verify(beneficiaryAdapter).getPatientsByContract(anyString());
        verify(patientClaimsProcessor, never()).process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private List<Consent> getConsents(GetPatientsByContractResponse patientsByContract) {
        return patientsByContract.getPatients()
                .stream().map(p -> p.getPatientId())
                .map(patientId ->  createConsent(patientId))
                .collect(Collectors.toList());
    }

    private Consent createConsent(String p) {
        Consent consent = new Consent();
        consent.setHicn(p);
        consent.setEffectiveDate(LocalDate.now().minusDays(10));
        return consent;
    }


    private Sponsor createSponsor() {
        Sponsor sponsor = new Sponsor();
        sponsor.setId(1L);
        sponsor.setOrgName("Hogwarts School of Wizardry");
        sponsor.setLegalName("Hogwarts School of Wizardry LLC");
        sponsor.setHpmsId(1);
        return sponsor;
    }

    private User createUser(Sponsor sponsor) {
        User user = new User();
        user.setId(1L);
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.com");
        user.setEnabled(TRUE);
        user.setSponsor(sponsor);
        return user;
    }

    private Contract createContract(Sponsor sponsor) {
        final int anInt = random.nextInt(64);
        Contract contract = new Contract();
        contract.setId(Long.valueOf(anInt));
        contract.setContractName("CONTRACT_" + anInt);
        contract.setContractNumber("" + anInt);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return contract;
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setId(1L);
        job.setJobUuid("S001");
        job.setStatusMessage("0%");
        job.setUser(user);
        return job;
    }

    private GetPatientsByContractResponse createPatientsByContractResponse(Contract contract) {
        return GetPatientsByContractResponse.builder()
                .contractNumber(contract.getContractNumber())
                .patient(toPatientDTO())
                .patient(toPatientDTO())
                .patient(toPatientDTO())
                .build();
    }

    private PatientDTO toPatientDTO() {
        final int anInt = random.nextInt(11);
        return PatientDTO.builder()
                .patientId("patient_" + anInt)
                .monthUnderContract(anInt)
                .build();
    }

}