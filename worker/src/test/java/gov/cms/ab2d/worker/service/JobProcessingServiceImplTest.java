package gov.cms.ab2d.worker.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.AsyncResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class JobProcessingServiceImplTest {

    @Autowired
    private FhirContext fhirContext;

    @Mock FileService fileService;
    @Mock JobRepository jobRepository;
    @Mock BeneficiaryAdapter beneficiaryAdapter;
    @Mock BfdClientAdapter bfdClientAdapter;

    private Sponsor sponsor;
    private User user;
    private Job job;

    private JobProcessingService cut;

    @BeforeEach
    void setUp() {
        cut = new JobProcessingServiceImpl(
                fhirContext,
                fileService,
                jobRepository,
                beneficiaryAdapter,
                bfdClientAdapter);

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
    void processJob() throws IOException {

        job.setStatus(JobStatus.IN_PROGRESS);
        var contract = createContract(sponsor);
        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var patientsByContract = createPatientsByContractResponse(contract);
        when(beneficiaryAdapter.getPatientsByContract(anyString())).thenReturn(patientsByContract);

        List<Bundle> resources = new ArrayList<>();
        resources.add(new Bundle());
        Future<List<Resource>> futureResources = new AsyncResult(resources);
        when(bfdClientAdapter.getEobBundleResources(anyString())).thenReturn(futureResources);

        var processedJob = cut.processJob("S001");

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());

        verify(fileService).createDirectory(Mockito.any());
        verify(fileService).createFile(Mockito.any(), Mockito.any());
        verify(fileService).appendToFile(Mockito.any(), Mockito.any());
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
        user.setUserName("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.com");
        user.setEnabled(TRUE);
        user.setSponsor(sponsor);
        return user;
    }

    private Contract createContract(Sponsor sponsor) {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setContractName("CONTRACT_1");
        contract.setContractNumber("1");
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
                .build();
    }

    private PatientDTO toPatientDTO() {
        return PatientDTO.builder()
                .patientId("patient_1")
                .monthUnderContract(1)
                .build();
    }

}