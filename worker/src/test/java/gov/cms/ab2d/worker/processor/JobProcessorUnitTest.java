package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.adapter.bluebutton.PatientClaimsProcessor;
import gov.cms.ab2d.worker.service.FileService;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
    @Mock private OptOutRepository optOutRepository;
    @Mock private ContractAdapter contractAdapter;
    @Mock private PatientClaimsProcessor patientClaimsProcessor;

    private Job job;
    private GetPatientsByContractResponse patientsByContract;


    @BeforeEach
    void setUp() throws IOException {
        cut = new JobProcessorImpl(
                fileService,
                jobRepository,
                jobOutputRepository,
                contractAdapter,
                patientClaimsProcessor,
                optOutRepository
        );

        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 2);
        ReflectionTestUtils.setField(cut, "efsMount", efsMountTmpDir.toString());

        final Sponsor parentSponsor = createParentSponsor();
        final Sponsor childSponsor = createChildSponsor(parentSponsor);
        final User user = createUser(childSponsor);
        job = createJob(user);

        var contract = createContract(childSponsor);
        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        patientsByContract = createPatientsByContractResponse(contract);
        Mockito.when(contractAdapter.getPatients(anyString())).thenReturn(patientsByContract);

        final Path outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        final Path outputDir = Files.createDirectories(outputDirPath);
        Mockito.lenient().when(fileService.createDirectory(Mockito.any(Path.class))).thenReturn(outputDir);
        Mockito.lenient().when(fileService.createOrReplaceFile(Mockito.any(Path.class), anyString()))
                .thenReturn(this.efsMountTmpDir)
                .thenReturn(this.efsMountTmpDir);

        Future<Integer> futureResources = new AsyncResult(0);
        Mockito.lenient().when(patientClaimsProcessor.process(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(futureResources);
    }



    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob_happyPath() {

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        doVerify();
    }


    @Test
    @DisplayName("When user belongs to a parent sponsor, contracts for the children sponsors are processed")
    void whenTheUserBelongsToParent_ChildContractsAreProcessed() {
        var user = job.getUser();

        //switch user to parent sponsor
        var childSponsor = user.getSponsor();
        var parent = childSponsor.getParent();
        user.setSponsor(parent);

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        assertThat(processedJob.getJobOutputs().size(), equalTo(childSponsor.getAttestedContracts().size()));
        doVerify();
    }


    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() {

        when(jobRepository.findJobStatus(anyString())).thenReturn(JobStatus.CANCELLED);

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(not(JobStatus.SUCCESSFUL)));
        assertThat(processedJob.getCompletedAt(), nullValue());
        doVerify();
    }

    @Test
    @DisplayName("When a job is submitted for a specific contract, process the export file for that contract only")
    void whenJobIsSubmittedForSpecificContract_processOnlyThatContract() {

        final Sponsor sponsor = job.getUser().getSponsor();

        // create 3 contract for the sponsor. But associate the submitted job with 1 specific contract.
        final Contract contract1 = createContract(sponsor);
        final Contract contract2 = createContract(sponsor);
        final Contract contract3 = createContract(sponsor);
        job.setContract(contract3);

        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        doVerify();
        verify(fileService, times(2)).createOrReplaceFile(Mockito.any(Path.class), anyString());
    }

    private void doVerify() {
        verify(fileService).createDirectory(Mockito.any());
        verify(contractAdapter).getPatients(anyString());
        verify(patientClaimsProcessor, atLeast(1)).process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    @DisplayName("When patient has opted out, their record will be skipped.")
    void processJob_whenPatientHasOptedOut_ShouldSkipPatientRecord() {

        final List<OptOut> optOuts = getOptOutRows(patientsByContract);
        when(optOutRepository.findByHicn(anyString()))
                .thenReturn(Arrays.asList(optOuts.get(0)))
                .thenReturn(Arrays.asList(optOuts.get(1)))
                .thenReturn(Arrays.asList(optOuts.get(2)));

        // Test data has 3 patientIds  each of whom has opted out.
        // So the patientsClaimsProcessor should never be called.
        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());

        verify(fileService).createDirectory(Mockito.any());
        verify(contractAdapter).getPatients(anyString());
        verify(patientClaimsProcessor, never()).process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    @DisplayName("When error counter for the patient claims is > 0, an extra job_output is created for the error file")
    void whenErrorCountIsGreaterThanZero_AJobOutputForErrorFileIsCreated() {

        Future<Integer> futureResources = new AsyncResult(1);
        Mockito.lenient().when(patientClaimsProcessor.process(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(futureResources);

        var processedJob = cut.process(jobUuid);

        var errorJobOutputs = processedJob.getJobOutputs()
                .stream()
                .filter(jobOutput -> jobOutput.isError())
                .collect(Collectors.toList());

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertFalse(errorJobOutputs.isEmpty());
        assertThat(processedJob.getCompletedAt(), notNullValue());
        doVerify();
    }

    @Test
    @DisplayName("When patientClaimsProcessor throws an exception, the job status becomes FAILED")
    void whenPatientClaimsProcessorThrowsException_jobFailsWithErrorMessage() {

        final String errMsg = "error during exception handling to write error record";
        final RuntimeException runtimeException = new RuntimeException(errMsg);
        Mockito.when(patientClaimsProcessor.process(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenThrow(runtimeException);


        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.FAILED));
        assertThat(processedJob.getStatusMessage(), is(errMsg));
        assertThat(processedJob.getCompletedAt(), notNullValue());
        doVerify();
    }

    @Test
    @DisplayName("When output directory for the job already exists, delete it and create it afresh")
    void whenOutputDirectoryAlreadyExist_DeleteItAndCreateItAfresh() throws IOException {

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

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());

        verify(fileService, times(2)).createDirectory(Mockito.any());
        verify(contractAdapter).getPatients(anyString());
        verify(patientClaimsProcessor, atLeast(1)).process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    @DisplayName("When existing output directory has a file which is not a regular file, job fails gracefully")
    void whenExistingOutputDirectoryHasSubDirectory_JobFailsGracefully() throws IOException {

        //create output dir, so it already exists
        final Path outputDir = Paths.get(efsMountTmpDir.toString(), jobUuid);
        Files.createDirectories(outputDir);

        //add a file in the directory which is NOT a regular file, but a directory
        final Path aDirPath = Path.of(outputDir.toString(), "DIR_1.ndjson");
        Files.createDirectories(aDirPath);
        var errMsg = "Directory already exists";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);
        Mockito.lenient().when(contractAdapter.getPatients(anyString())).thenReturn(patientsByContract);

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.FAILED));
        assertThat(processedJob.getStatusMessage(), CoreMatchers.startsWith("Could not delete directory"));
        assertThat(processedJob.getExpiresAt(), nullValue());

        verify(fileService).createDirectory(Mockito.any());
        verify(contractAdapter, never()).getPatients(anyString());
        verify(fileService, never()).createOrReplaceFile(Mockito.any(Path.class), anyString());
    }

    @Test
    @DisplayName("When output directory creation fails due to unknown IOException, job fails gracefully")
    void whenOutputDirectoryCreationFailsDueToUnknownReason_JobFailsGracefully() throws IOException {

        var errMsg = "Could not create output directory";
        var uncheckedIOE = new UncheckedIOException(errMsg, new IOException(errMsg));

        Mockito.when(fileService.createDirectory(any())).thenThrow(uncheckedIOE);
        Mockito.lenient().when(contractAdapter.getPatients(anyString())).thenReturn(patientsByContract);

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.FAILED));
        assertThat(processedJob.getStatusMessage(), CoreMatchers.startsWith("Could not create output directory"));
        assertThat(processedJob.getExpiresAt(), nullValue());

        verify(fileService).createDirectory(Mockito.any());
        verify(contractAdapter, never()).getPatients(anyString());
        verify(fileService, never()).createOrReplaceFile(Mockito.any(Path.class), anyString());
    }


    private List<OptOut> getOptOutRows(GetPatientsByContractResponse patientsByContract) {
        return patientsByContract.getPatients()
                .stream().map(p -> p.getPatientId())
                .map(patientId ->  createOptOut(patientId))
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
        job.setStatus(JobStatus.IN_PROGRESS);
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