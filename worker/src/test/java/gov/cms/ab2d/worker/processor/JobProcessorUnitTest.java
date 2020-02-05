package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.service.FileService;
import org.hamcrest.CoreMatchers;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    @Mock private BFDClient mockBfdClient;

    private Job job;
    private GetPatientsByContractResponse patientsByContract;

    @BeforeEach
    void setUp() throws Exception {
        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        Mockito.lenient().when(mockBfdClient.requestEOBFromServer(anyString())).thenReturn(bundle1);

        FhirContext fhirContext = new FhirContext();
        PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorImpl(mockBfdClient, fhirContext);

        cut = new JobProcessorImpl(
                fileService,
                jobRepository,
                jobOutputRepository,
                contractAdapter,
                patientClaimsProcessor,
                optOutRepository
        );

        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressLogFrequency", 3);
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);
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
        Mockito.lenient().when(fileService.createDirectory(any(Path.class))).thenReturn(outputDir);
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob_happyPath() {

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
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
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
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
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
    }

    @Test
    @DisplayName("When a job is submitted for a specific contract, process the export file for that contract only")
    void whenJobIsSubmittedForSpecificContract_processOnlyThatContract() {

        final Sponsor sponsor = job.getUser().getSponsor();
        final Contract contract = sponsor.getAttestedContracts().get(0);

        // create 3 additional contracts for the sponsor.
        // But associate the submitted job with the (original) contract for which PatientsByContractResponse test data was setup
        final Contract contract1 = createContract(sponsor);
        final Contract contract2 = createContract(sponsor);
        final Contract contract3 = createContract(sponsor);
        job.setContract(contract);

        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        doVerify();
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
    }

    private void doVerify() {
        verify(fileService).createDirectory(any());
        verify(contractAdapter).getPatients(anyString());
    }

    @Test
    @DisplayName("When patient has opted out, their record will be skipped.")
    void processJob_whenSomePatientHasOptedOut_ShouldSkipThatPatientRecord() {

        final List<OptOut> optOuts = getOptOutRows(patientsByContract);
        when(optOutRepository.findByCcwId(anyString()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(optOuts.get(1)))
                .thenReturn(Arrays.asList(optOuts.get(2)));

        // Test data has 3 patientIds  each of whom has opted out.
        // So the patientsClaimsProcessor should never be called.
        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());

        verify(fileService).createDirectory(any());
        verify(contractAdapter).getPatients(anyString());
    }

    @Test
    @DisplayName("When all patients have opted out, their record will be skipped and job FAILS as no jobOutput rows were created")
    void processJob_whenAllPatientsHaveOptedOut_SkipTheirRecordsAndfailJobAsNoJobOutputRowsCreated() {

        final List<OptOut> optOuts = getOptOutRows(patientsByContract);
        when(optOutRepository.findByCcwId(anyString()))
                .thenReturn(Arrays.asList(optOuts.get(0)))
                .thenReturn(Arrays.asList(optOuts.get(1)))
                .thenReturn(Arrays.asList(optOuts.get(2)));

        // Test data has 3 patientIds  each of whom has opted out.
        // So the patientsClaimsProcessor should never be called.
        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.FAILED));
        assertThat(processedJob.getStatusMessage(), is("No JobOutput records were created."));

        verify(fileService).createDirectory(any());
        verify(contractAdapter).getPatients(anyString());
    }


//    @Test
//    @DisplayName("When patientClaimsProcessor throws an exception, the job status becomes FAILED")
//    void whenPatientClaimsProcessorThrowsException_jobFailsWithErrorMessage() {
//
//        final String errMsg = "error during exception handling to write error record";
//        final RuntimeException runtimeException = new RuntimeException(errMsg);
////        Mockito.when(patientClaimsProcessor.process(any(), any(), any())).thenThrow(runtimeException);
//
//        var processedJob = cut.process(jobUuid);
//
//        assertThat(processedJob.getStatus(), is(JobStatus.FAILED));
//        assertThat(processedJob.getStatusMessage(), is(errMsg));
//        assertThat(processedJob.getCompletedAt(), notNullValue());
//        doVerify();
//    }

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

        verify(fileService, times(2)).createDirectory(any());
        verify(contractAdapter).getPatients(anyString());
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
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

        verify(fileService).createDirectory(any());
        verify(contractAdapter, never()).getPatients(anyString());
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

        verify(fileService).createDirectory(any());
        verify(contractAdapter, never()).getPatients(anyString());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() throws ParseException {

        var contract = job.getUser().getSponsor().getContracts().iterator().next();
        var patients = createPatientsByContractResponse(contract).getPatients();
        var manyPatientIds = new ArrayList<PatientDTO>();
        manyPatientIds.addAll(patients);
        manyPatientIds.addAll(patients);
        manyPatientIds.addAll(patients);
        manyPatientIds.addAll(patients);
        manyPatientIds.addAll(patients);
        manyPatientIds.addAll(patients);
        patientsByContract.setPatients(manyPatientIds);
        var processedJob = cut.process(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), is("100%"));
        assertThat(processedJob.getExpiresAt(), notNullValue());
        verify(jobRepository, times(9)).updatePercentageCompleted(anyString(), anyInt());
        doVerify();
    }

    private List<OptOut> getOptOutRows(GetPatientsByContractResponse patientsByContract) {
        return patientsByContract.getPatients()
                .stream().map(PatientDTO::getPatientId)
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
        user.setEmail("harry_potter@hogwarts.com");
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

    private GetPatientsByContractResponse createPatientsByContractResponse(Contract contract) throws ParseException {
        return GetPatientsByContractResponse.builder()
                .contractNumber(contract.getContractNumber())
                .patient(toPatientDTO())
                .patient(toPatientDTO())
                .patient(toPatientDTO())
                .build();
    }

    private PatientDTO toPatientDTO() throws ParseException {
        final int anInt = random.nextInt(11);
        return PatientDTO.builder()
                .patientId("patient_" + anInt)
                .datesUnderContract(new FilterOutByDate.DateRange(new Date(0), new Date()))
                .build();
    }



}