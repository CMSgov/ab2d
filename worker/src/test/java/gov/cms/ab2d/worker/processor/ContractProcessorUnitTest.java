package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.PatientDTO;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import gov.cms.ab2d.worker.processor.stub.PatientClaimsProcessorStub;
import gov.cms.ab2d.worker.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;

import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractProcessorUnitTest {
    // class under test
    private ContractProcessor cut;

    @TempDir Path efsMountTmpDir;

    @Mock private FileService fileService;
    @Mock private JobRepository jobRepository;
    @Mock private LogManager eventLogger;
    private PatientClaimsProcessor patientClaimsProcessor = spy(PatientClaimsProcessorStub.class);

    private ContractBeneficiaries patientsByContract;
    private Path outputDir;
    private ContractData contractData;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";
        FhirContext fhirContext = ca.uhn.fhir.context.FhirContext.forDstu3();
        cut = new ContractProcessorImpl(
                fileService,
                jobRepository,
                patientClaimsProcessor,
                eventLogger,
                fhirContext
        );
        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressLogFrequency", 3);
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);

        var user = createUser();
        var job = createJob(user);
        var contract = createContract();

        patientsByContract = createPatientsByContractResponse(contract);

        var outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        outputDir = Files.createDirectories(outputDirPath);

        ProgressTracker progressTracker = ProgressTracker.builder()
                .jobUuid(jobUuid)
                .numContracts(1)
                .failureThreshold(10)
                .build();
        progressTracker.addPatientsByContract(patientsByContract);
        contractData = new ContractData(contract, progressTracker, job.getSince(),
                job.getUser() != null ? job.getUser().getUsername() : null);
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() {
        when(jobRepository.findJobStatus(anyString())).thenReturn(JobStatus.CANCELLED);

        var exceptionThrown = assertThrows(JobCancelledException.class,
                () -> cut.process(outputDir, contractData));

        assertTrue(exceptionThrown.getMessage().startsWith("Job was cancelled while it was being processed"));
        verify(patientClaimsProcessor, atLeast(1)).process(any());
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() throws Exception {
        patientsByContract.setPatients(createPatients(18));
        var jobOutputs = cut.process(outputDir, contractData);

        assertFalse(jobOutputs.isEmpty());
        verify(jobRepository, times(9)).updatePercentageCompleted(anyString(), anyInt());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    private User createUser() {
        User user = new User();
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.edu");
        user.setEnabled(TRUE);
        user.setContract(createContract());
        return user;
    }

    private Contract createContract() {
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_NM_00000");
        contract.setContractNumber("CONTRACT_00000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));

        return contract;
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatusMessage("0%");
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setUser(user);
        return job;
    }

    private ContractBeneficiaries createPatientsByContractResponse(Contract contract) throws ParseException {
        Map<String, PatientDTO> patientMap = createPatients(3);
        return ContractBeneficiaries.builder()
                .contractNumber(contract.getContractNumber())
                .patients(patientMap)
                .build();
    }

    private Map<String, PatientDTO> createPatients(int num) throws ParseException {
        FilterOutByDate.DateRange dateRange = TestUtil.getOpenRange();
        Map<String, PatientDTO> patients = new HashMap<>();
        for (int i = 0; i < num; i++) {
            PatientDTO p = PatientDTO.builder()
                    .dateRangesUnderContract(Collections.singletonList(dateRange))
                    .identifiers(createIdentifierWithoutMbi("patient_" + i)).build();
            patients.put(p.getBeneficiaryId(), p);
        }
        return patients;
    }
}