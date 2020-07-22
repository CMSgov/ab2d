package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.ab2d.worker.processor.StreamHelperImpl.FileOutputType.NDJSON;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractProcessorUnitTest {
    // class under test
    private ContractProcessor cut;

    private String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    private Random random = new Random();

    @TempDir Path efsMountTmpDir;

    @Mock private FileService fileService;
    @Mock private JobRepository jobRepository;
    @Mock private OptOutRepository optOutRepository;
    @Mock private LogManager eventLogger;
    private PatientClaimsProcessor patientClaimsProcessor = spy(PatientClaimsProcessorStub.class);

    private ContractBeneficiaries patientsByContract;
    private Path outputDir;
    private ContractData contractData;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        cut = new ContractProcessorImpl(
                fileService,
                jobRepository,
                patientClaimsProcessor,
                optOutRepository,
                eventLogger
        );
        ReflectionTestUtils.setField(cut, "optoutUsed", true);
        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressLogFrequency", 3);
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);

        var parentSponsor = createParentSponsor();
        var childSponsor = createChildSponsor(parentSponsor);
        var user = createUser(childSponsor);
        var job = createJob(user);
        var contract = createContract(childSponsor);

        patientsByContract = createPatientsByContractResponse(contract);

        var outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        outputDir = Files.createDirectories(outputDirPath);

        ProgressTracker progressTracker = ProgressTracker.builder()
                .jobUuid(jobUuid)
                .failureThreshold(10)
                .build();
        progressTracker.addPatientsByContract(patientsByContract);
        contractData = new ContractData(contract, progressTracker, contract.getAttestedOn(), job.getSince(),
                job.getUser() != null ? job.getUser().getUsername() : null);
    }


    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() throws Exception {
        when(jobRepository.findJobStatus(anyString())).thenReturn(JobStatus.CANCELLED);

        var exceptionThrown = assertThrows(JobCancelledException.class,
                () -> cut.process(outputDir, contractData, NDJSON));

        assertThat(exceptionThrown.getMessage(), startsWith("Job was cancelled while it was being processed"));
        verify(patientClaimsProcessor, atLeast(1)).process(any());
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
    }

    @Test
    @DisplayName("When patient has opted out, but opt out is disabled their record will not be skipped.")
    void processJob_whenSomePatientHasOptedOut_But_OptOut_Off_ShouldNotSkipThatPatientRecord() throws Exception {
        ReflectionTestUtils.setField(cut, "optoutUsed", false);
        final List<OptOut> optOuts = getOptOutRows(patientsByContract);
        lenient().when(optOutRepository.findByCcwId(anyString()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(optOuts.get(1)))
                .thenReturn(Arrays.asList(optOuts.get(2)));

        List<JobOutput> jobOutputs = cut.process(outputDir, contractData, NDJSON);
        // Verify that all outputs are returned
        assertEquals(6, jobOutputs.size());
    }

    @Test
    @DisplayName("When patient has opted out, their record will be skipped.")
    void processJob_whenSomePatientHasOptedOut_ShouldSkipThatPatientRecord() throws Exception {
        final List<OptOut> optOuts = getOptOutRows(patientsByContract);
        when(optOutRepository.findByCcwId(anyString()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(optOuts.get(1)))
                .thenReturn(Arrays.asList(optOuts.get(2)));

        var jobOutputs = cut.process(outputDir, contractData, NDJSON);

        assertFalse(jobOutputs.isEmpty());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    @Test
    @DisplayName("When all patients have opted out, should throw exception as no jobOutput rows were created")
    void processJob_whenAllPatientsHaveOptedOut_ShouldThrowException() throws Exception {
        final List<OptOut> optOuts = getOptOutRows(patientsByContract);
        when(optOutRepository.findByCcwId(anyString()))
                .thenReturn(Arrays.asList(optOuts.get(0)))
                .thenReturn(Arrays.asList(optOuts.get(1)))
                .thenReturn(Arrays.asList(optOuts.get(2)));

        // Test data has 3 patientIds each of whom has opted out.
        // So the patientsClaimsProcessor should never be called.
        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.process(outputDir, contractData, NDJSON));

        assertThat(exceptionThrown.getMessage(), startsWith("The export process has produced no results"));
        verify(patientClaimsProcessor, never()).process(any());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() throws Exception {
        patientsByContract.setPatients(createPatients(18));
        var jobOutputs = cut.process(outputDir, contractData, NDJSON);

        assertFalse(jobOutputs.isEmpty());
        verify(jobRepository, times(9)).updatePercentageCompleted(anyString(), anyInt());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    private List<OptOut> getOptOutRows(ContractBeneficiaries patientsByContract) {
        return patientsByContract.getPatients().keySet().stream()
                .map(this::createOptOut)
                .collect(Collectors.toList());
    }

    private OptOut createOptOut(String patientId) {
        OptOut optOut = new OptOut();
        optOut.setHicn(patientId);
        optOut.setEffectiveDate(LocalDate.now().minusDays(10));
        return optOut;
    }

    private Sponsor createParentSponsor() {
        Sponsor parentSponsor = new Sponsor();
        parentSponsor.setOrgName("PARENT");
        parentSponsor.setLegalName("LEGAL PARENT");
        return parentSponsor;
    }

    private Sponsor createChildSponsor(Sponsor parentSponsor) {
        Sponsor childSponsor = new Sponsor();
        childSponsor.setOrgName("Hogwarts School of Wizardry");
        childSponsor.setLegalName("Hogwarts School of Wizardry LLC");

        childSponsor.setParent(parentSponsor);
        parentSponsor.getChildren().add(childSponsor);

        return childSponsor;
    }

    private User createUser(Sponsor sponsor) {
        User user = new User();
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.edu");
        user.setEnabled(TRUE);
        user.setSponsor(sponsor);
        return user;
    }

    private Contract createContract(Sponsor sponsor) {
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_NM_00000");
        contract.setContractNumber("CONTRACT_00000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
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
        FilterOutByDate.DateRange dateRange = new FilterOutByDate.DateRange(new Date(0), new Date());
        Map<String, PatientDTO> patients = new HashMap<>();
        for (int i = 0; i < num; i++) {
            PatientDTO p = PatientDTO.builder()
                    .dateRangesUnderContract(Collections.singletonList(dateRange))
                    .patientId("patient_" + i).build();
            patients.put(p.getPatientId(), p);
        }
        return patients;
    }
}