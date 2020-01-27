package gov.cms.ab2d.worker.processor.contract;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobProgress;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobProgressRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.JobDM;
import gov.cms.ab2d.worker.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ContractSliceProcessorTest {

    private static final int PATIENT_COUNT_IN_SLICE = 10;
    private ContractSliceProcessor cut;

    @TempDir
    Path efsMountTmpDir;

    private String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @Mock FileService fileService;
    @Mock JobRepository jobRepository;
    @Mock JobProgressRepository jobProgressRepository;
    @Mock JobOutputRepository jobOutputRepository;
    @Mock OptOutRepository optOutRepository;
    @Mock PatientClaimsProcessor patientClaimsProcessor;

    @Captor
    private ArgumentCaptor<List<JobOutput>> captor;

    private Job job;
    private Map.Entry<Integer, List<PatientDTO>> slice;
    private ContractData contractData;


    @BeforeEach
    void setUp() throws IOException {
        cut = new ContractSliceProcessorImpl(fileService,
                jobRepository,
                jobProgressRepository,
                jobOutputRepository,
                optOutRepository,
                patientClaimsProcessor
        );

        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressLogFrequency", 2);

        final Sponsor childSponsor = createChildSponsor();
        final User user = createUser(childSponsor);
        job = createJob(user);

        var contract = createContract(childSponsor);
        var slices = new HashMap<Integer, List<PatientDTO>>();
        slices.put(1, createPatients(PATIENT_COUNT_IN_SLICE));

        JobDM jobDM = createJobDM(contract, slices);
        contractData = new ContractData(efsMountTmpDir, contract, jobDM);

        slice = slices.entrySet().iterator().next();

        final Path dataFile = createFile(efsMountTmpDir, "datafile.ndjson");
        final Path errorFile = createFile(efsMountTmpDir, "errorfile.ndjson");
        when(jobRepository.findJobStatus(Mockito.any())).thenReturn(JobStatus.IN_PROGRESS);

        Mockito.lenient().when(fileService.createOrReplaceFile(any(), any()))
                .thenReturn(dataFile)
                .thenReturn(errorFile);
        Mockito.lenient().when(jobRepository.findByJobUuid(any())).thenReturn(job);

        JobProgress jobProgress = createJobProgress();
        Mockito.lenient().when(jobProgressRepository.findOne(any(), any(), any())).thenReturn(jobProgress);
        Mockito.lenient().when(patientClaimsProcessor.process(any(), any(), any(), any())).thenReturn(0);
    }


    @Test
    @DisplayName("Stop processing and exit immediately if jobStatus is Cancelled")
    void when_jobStatusIsCancelled_InProgress_ExitWithoutProcessing() throws ExecutionException, InterruptedException {
        when(jobRepository.findJobStatus(Mockito.any())).thenReturn(JobStatus.CANCELLED);
        cut.process(slice, contractData).get();
        verifyImmediateExit();
    }

    @Test
    @DisplayName("Stop processing and exit immediately if jobStatus is Failed")
    void when_jobStatusIsFailed_InProgress_ExitWithoutProcessing() throws ExecutionException, InterruptedException {
        when(jobRepository.findJobStatus(Mockito.any())).thenReturn(JobStatus.FAILED);
        cut.process(slice, contractData).get();
        verifyImmediateExit();
    }

    private void verifyImmediateExit() {
        verify(fileService, never()).createOrReplaceFile(any(), any());
        verify(jobProgressRepository, never()).findOne(any(), any(), any());
        verify(jobProgressRepository, never()).saveAndFlush(any());
        verify(optOutRepository, never()).findByHicn(any());
        verify(jobRepository, never()).findByJobUuid(any());
        verify(jobOutputRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("when patient has opted out, skip processing the patient's claim information")
    void whenPatientHasOptedOut_skipPatient() throws ExecutionException, InterruptedException, IOException {
        OptOut optOut = new OptOut();
        optOut.setEffectiveDate(LocalDate.now().minusDays(2));
        when(optOutRepository.findByHicn(any())).thenReturn(List.of(optOut));

        cut.process(slice, contractData).get();

        verify(fileService, times(2)).createOrReplaceFile(any(), any());
        verify(jobProgressRepository).findOne(any(), any(), any());
        verify(jobProgressRepository, times(5)).saveAndFlush(any());
        verify(optOutRepository, times(PATIENT_COUNT_IN_SLICE)).findByHicn(any());
        verify(patientClaimsProcessor, never()).process(any(), any(), any(), any());
        verify(jobRepository).findByJobUuid(any());
        verify(jobOutputRepository).saveAll(any());
    }

    @Test
    @DisplayName("when PatientClaimsProcessor throws Exception, puts job in failed status")
    void whenPatientClaimsProcessThrowsException_putsJobInFailedStatus() throws ExecutionException, InterruptedException, IOException {

        when(patientClaimsProcessor.process(any(), any(), any(), any())).thenThrow(new RuntimeException("TEST"));

        cut.process(slice, contractData).get();

        verify(fileService, times(2)).createOrReplaceFile(any(), any());
        verify(jobProgressRepository).findOne(any(), any(), any());
        verify(optOutRepository).findByHicn(any());
        verify(patientClaimsProcessor).process(any(), any(), any(), any());
        verify(jobRepository, never()).findByJobUuid(any());
        verify(jobOutputRepository, never()).saveAll(any());
        verify(jobRepository).saveJobFailure(any(), any());
    }

    @Test
    @DisplayName("When job status changes to not in_progress midstream, stops processing the job")
    void whenJobStatusChangesToNotInProgresMidstream_StopsProcessingJob() throws ExecutionException, InterruptedException, IOException {

        when(jobRepository.findJobStatus(any()))
                .thenReturn(JobStatus.IN_PROGRESS)
                .thenReturn(JobStatus.FAILED);

        cut.process(slice, contractData).get();

        verify(fileService, times(2)).createOrReplaceFile(any(), any());
        verify(jobProgressRepository).findOne(any(), any(), any());
        verify(optOutRepository).findByHicn(any());
        verify(patientClaimsProcessor).process(any(), any(), any(), any());
        verify(jobRepository).findByJobUuid(any());
        verify(jobOutputRepository).saveAll(any());
        verify(jobRepository, never()).saveJobFailure(any(), any());
    }

    @Test
    @DisplayName("when any patient who has a non-fatal error, an error jobOutput is created at the end of the job")
    void whenAPatientHasNonFatalError_createsAnErrorJobOutput() throws ExecutionException, InterruptedException {
        slice.setValue(createPatients(1));
        when(patientClaimsProcessor.process(any(), any(), any(), any())).thenReturn(1);
        cut.process(slice, contractData).get();

        verify(fileService, times(2)).createOrReplaceFile(any(), any());
        verify(jobProgressRepository).findOne(any(), any(), any());
        verify(jobProgressRepository, times(1)).saveAndFlush(any());
        verify(optOutRepository, times(1)).findByHicn(any());
        verify(patientClaimsProcessor, times(1)).process(any(), any(), any(), any());
        verify(jobRepository).findByJobUuid(any());
        verify(jobOutputRepository, times(1)).saveAll(captor.capture());

        final List<JobOutput> capturedJobOutputs = captor.getValue();
        assertThat(capturedJobOutputs.size(), is(1));
        capturedJobOutputs.forEach(jobOutput -> {
            assertThat(jobOutput.getError(), is(TRUE));
            assertThat(jobOutput.getFilePath(), is("errorfile.ndjson"));
        });
    }

    @Test
    @DisplayName("when some patients have a non-fatal error, 2 jobOutputs are created - 1 for success & other for failure")
    void whenSomePatientsHaveNonFatalError_createsTwoJobOutput() throws ExecutionException, InterruptedException {
        slice.setValue(createPatients(2));
        when(patientClaimsProcessor.process(any(), any(), any(), any()))
                .thenReturn(0)
                .thenReturn(1);
        cut.process(slice, contractData).get();

        verify(fileService, times(2)).createOrReplaceFile(any(), any());
        verify(jobProgressRepository).findOne(any(), any(), any());
        verify(jobProgressRepository, times(1)).saveAndFlush(any());
        verify(optOutRepository, times(2)).findByHicn(any());
        verify(patientClaimsProcessor, times(2)).process(any(), any(), any(), any());
        verify(jobRepository).findByJobUuid(any());
        verify(jobOutputRepository, times(1)).saveAll(captor.capture());

        final List<JobOutput> capturedJobOutputs = captor.getValue();
        assertThat(capturedJobOutputs.size(), is(2));

        final JobOutput jobOutput0 = capturedJobOutputs.get(0);
        assertThat(jobOutput0.getError(), is(FALSE));
        assertThat(jobOutput0.getFilePath(), is("datafile.ndjson"));

        final JobOutput jobOutput1 = capturedJobOutputs.get(1);
        assertThat(jobOutput1.getError(), is(TRUE));
        assertThat(jobOutput1.getFilePath(), is("errorfile.ndjson"));
    }

    @Test
    @DisplayName("given a slice of patients, they are all processed")
    void happyPath() throws ExecutionException, InterruptedException {
        cut.process(slice, contractData).get();

        verify(fileService, times(2)).createOrReplaceFile(any(), any());
        verify(jobProgressRepository).findOne(any(), any(), any());
        verify(jobProgressRepository, times(5)).saveAndFlush(any());
        verify(optOutRepository, times(PATIENT_COUNT_IN_SLICE)).findByHicn(any());
        verify(patientClaimsProcessor, times(PATIENT_COUNT_IN_SLICE)).process(any(), any(), any(), any());
        verify(jobRepository).findByJobUuid(any());

        verify(jobOutputRepository, times(1)).saveAll(captor.capture());

        final List<JobOutput> capturedJobOutputs = captor.getValue();
        assertThat(capturedJobOutputs.size(), is(1));
        capturedJobOutputs.forEach(jobOutput -> {
            assertThat(jobOutput.getError(), is(FALSE));
            assertThat(jobOutput.getFilePath(), is("datafile.ndjson"));
        });
    }



    private List<PatientDTO> createPatients(int maxVal) {
        return SliceCreatorTestUtil.createPatients(maxVal);
    }

    private Path createFile(Path path, String outputFilename) throws IOException {
        final Path filePath = Path.of(path.toString(), outputFilename);
        return Files.createFile(filePath);
    }

    private JobDM createJobDM(Contract contract, Map<Integer, List<PatientDTO>> slices) {
        return JobDM.builder()
                .jobId(job.getId())
                .jobUuid(jobUuid)
                .contract(toContractDM(contract, slices))
                .build();
    }

    private JobDM.ContractDM toContractDM(Contract contract, Map<Integer, List<PatientDTO>> slices) {
        return JobDM.ContractDM.builder()
                .contractId(contract.getId())
                .contractNumber(contract.getContractNumber())
                .slices(slices)
                .build();
    }

    private Sponsor createChildSponsor() {

        Sponsor childSponsor = new Sponsor();
        childSponsor.setOrgName("Hogwarts School of Wizardry");
        childSponsor.setLegalName("Hogwarts School of Wizardry LLC");

        final Sponsor parentSponsor = createParentSponsor();
        childSponsor.setParent(parentSponsor);
        parentSponsor.getChildren().add(childSponsor);

        return childSponsor;
    }

    private Sponsor createParentSponsor() {
        Sponsor parentSponsor = new Sponsor();
        parentSponsor.setOrgName("PARENT");
        parentSponsor.setLegalName("LEGAL PARENT");
        return parentSponsor;
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
        contract.setContractName("CONTRACT_NAME_00000");
        contract.setContractNumber("CONTRACT_NO_00000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return contract;
    }

    private JobProgress createJobProgress() {
        JobProgress jobProgress = new JobProgress();
        jobProgress.setSliceNumber(1);
        jobProgress.setSliceTotal(slice.getValue().size());
        jobProgress.setRecordsProcessed(0);
        return jobProgress;
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid(jobUuid);
        job.setStatusMessage("0%");
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setUser(user);
        return job;
    }


}