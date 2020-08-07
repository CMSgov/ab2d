package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneSearch;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.PatientDTO;
import gov.cms.ab2d.worker.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobProcessorUnitTest {
    // class under test
    private JobProcessor cut;

    private String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    private Random random = new Random();

    @TempDir Path efsMountTmpDir;

    @Mock private FileService fileService;
    @Mock private JobRepository jobRepository;
    @Mock private JobOutputRepository jobOutputRepository;
    @Mock private ContractBeneSearch contractBeneSearch;
    @Mock private PatientClaimsProcessor patientClaimsProcessor;
    @Mock private LogManager eventLogger;
    @Mock private BFDClient bfdClient;

    private Job job;
    private ContractBeneficiaries patientsByContract;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ThreadPoolTaskExecutor patientContractThreadPool = new ThreadPoolTaskExecutor();
        patientContractThreadPool.setCorePoolSize(6);
        patientContractThreadPool.setMaxPoolSize(12);
        patientContractThreadPool.setThreadNamePrefix("jobproc-");
        patientContractThreadPool.initialize();

        cut = new JobProcessorImpl(
                fileService,
                jobRepository,
                jobOutputRepository,
                eventLogger,
                bfdClient,
                patientClaimsProcessor,
                patientContractThreadPool
        );

        ReflectionTestUtils.setField(cut, "efsMount", efsMountTmpDir.toString());

        final Sponsor parentSponsor = createParentSponsor();
        final Sponsor childSponsor = createChildSponsor(parentSponsor);
        final User user = createUser(childSponsor);
        job = createJob(user);

        var contract = createContract(childSponsor);
        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        patientsByContract = createPatientsByContractResponse(contract);
        Mockito.when(contractBeneSearch.getPatients(anyString(), anyInt(), any())).thenReturn(patientsByContract);

        final Path outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        final Path outputDir = Files.createDirectories(outputDirPath);
        Mockito.lenient().when(fileService.createDirectory(any(Path.class))).thenReturn(outputDir);
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob_happyPath() throws ExecutionException, InterruptedException {

        var processedJob = cut.process(jobUuid);

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    @Test
    @DisplayName("When user belongs to a parent sponsor, contracts for the children sponsors are processed")
    void whenTheUserBelongsToParent_ChildContractsAreProcessed() throws ExecutionException, InterruptedException {
        var user = job.getUser();

        //switch user to parent sponsor
        var childSponsor = user.getSponsor();
        var parent = childSponsor.getParent();
        user.setSponsor(parent);

        var processedJob = cut.process(jobUuid);

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    @Test
    @DisplayName("When a job is submitted for a specific contract, process the export file for that contract only")
    void whenJobIsSubmittedForSpecificContract_processOnlyThatContract() throws ExecutionException, InterruptedException {

        final Sponsor sponsor = job.getUser().getSponsor();
        final Contract contract = sponsor.getAttestedContracts().get(0);

        // create 3 additional contracts for the sponsor.
        // But associate the submitted job with the (original) contract for which PatientsByContractResponse test data was setup
        createContract(sponsor);
        createContract(sponsor);
        createContract(sponsor);
        job.setContract(contract);

        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var processedJob = cut.process(jobUuid);

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        doVerify();
    }

    private void doVerify() throws ExecutionException, InterruptedException {
        verify(fileService).createDirectory(any());
        verify(contractBeneSearch).getPatients(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("When output directory for the job already exists, delete it and create it afresh")
    void whenOutputDirectoryAlreadyExist_DeleteItAndCreateItAfresh() throws IOException, ExecutionException, InterruptedException {

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), jobUuid);
        Files.createDirectories(outputDir);

        //create files inside the directory.
        final Path filePath1 = Path.of(outputDir.toString(), "FILE_1.ndjson");
        Files.createFile(filePath1);

        final Path filePath2 = Path.of(outputDir.toString(), "FILE_2.ndjson");
        Files.createFile(filePath2);

        var errMsg = "Directory already exists";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));
        Mockito.when(fileService.createDirectory(any()))
                .thenThrow(uncheckedIOE)
                .thenReturn(efsMountTmpDir);

        var processedJob = cut.process(jobUuid);

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());

        verify(fileService, times(2)).createDirectory(any());
        verify(contractBeneSearch).getPatients(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("When existing output directory has a file which is not a regular file, job fails gracefully")
    void whenExistingOutputDirectoryHasSubDirectory_JobFailsGracefully() throws IOException, ExecutionException, InterruptedException {

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), jobUuid);
        Files.createDirectories(outputDir);

        //add a file in the directory which is NOT a regular file, but a directory
        final Path aDirPath = Path.of(outputDir.toString(), "DIR_1.ndjson");
        Files.createDirectories(aDirPath);
        var errMsg = "Directory already exists";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);
        Mockito.lenient().when(contractBeneSearch.getPatients(anyString(), anyInt(), any())).thenReturn(patientsByContract);

        var processedJob = cut.process(jobUuid);

        assertEquals(processedJob.getStatus(), JobStatus.FAILED);
        assertTrue(processedJob.getStatusMessage().startsWith("Could not delete"));
        assertNull(processedJob.getExpiresAt());

        verify(fileService).createDirectory(any());
        verify(contractBeneSearch, never()).getPatients(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("When output directory creation fails due to unknown IOException, job fails gracefully")
    void whenOutputDirectoryCreationFailsDueToUnknownReason_JobFailsGracefully() throws ExecutionException, InterruptedException {

        var errMsg = "Could not create output directory";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);
        Mockito.lenient().when(contractBeneSearch.getPatients(anyString(), anyInt(), any())).thenReturn(patientsByContract);

        var processedJob = cut.process(jobUuid);

        assertEquals(processedJob.getStatus(), JobStatus.FAILED);
        assertTrue(processedJob.getStatusMessage().startsWith("Could not create output directory"));
        assertNull(processedJob.getExpiresAt());

        verify(fileService).createDirectory(any());
        verify(contractBeneSearch, never()).getPatients(anyString(), anyInt(), any());
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
        PatientDTO p1 = toPatientDTO();
        PatientDTO p2 = toPatientDTO();
        PatientDTO p3 = toPatientDTO();

        return ContractBeneficiaries.builder()
                .contractNumber(contract.getContractNumber())
                .patient(p1.getPatientId(), p1)
                .patient(p2.getPatientId(), p2)
                .patient(p3.getPatientId(), p3)
                .build();
    }

    private PatientDTO toPatientDTO() throws ParseException {
        int anInt = random.nextInt(11);
        var dateRange = new FilterOutByDate.DateRange(new Date(0), new Date());
        return PatientDTO.builder()
                .patientId("patient_" + anInt)
                .dateRangesUnderContract(Collections.singletonList(dateRange))
                .build();
    }
}