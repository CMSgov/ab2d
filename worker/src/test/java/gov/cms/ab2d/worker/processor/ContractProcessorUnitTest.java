package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.stub.PatientClaimsProcessorStub;
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
import java.time.OffsetDateTime;
import java.util.*;

import static gov.cms.ab2d.common.util.EventUtils.getOrganization;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractProcessorUnitTest {

    private static final String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    // class under test
    private ContractProcessor cut;

    @TempDir Path efsMountTmpDir;

    @Mock private JobRepository jobRepository;
    @Mock private LogManager eventLogger;
    @Mock private RoundRobinBlockingQueue<PatientClaimsRequest> requestQueue;
    private PatientClaimsProcessor patientClaimsProcessor;

    private Path outputDir;
    private Contract contract;
    private Job job;
    private PdpClient pdpClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        patientClaimsProcessor = spy(PatientClaimsProcessorStub.class);

        cut = new ContractProcessorImpl(
                jobRepository,
                patientClaimsProcessor,
                eventLogger,
                requestQueue);
        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressDbFrequency", 2);
        ReflectionTestUtils.setField(cut, "reportProgressLogFrequency", 3);
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);

        pdpClient = createClient();
        job = createJob(pdpClient);
        contract = createContract();

        var outputDirPath = Paths.get(efsMountTmpDir.toString(), jobUuid);
        outputDir = Files.createDirectories(outputDirPath);
    }

    @Test
    @DisplayName("When a job is cancelled while it is being processed, then attempt to stop the job gracefully without completing it")
    void whenJobIsCancelledWhileItIsBeingProcessed_ThenAttemptToStopTheJob() {

        List<CoverageSummary> patientsByContract = createPatientsByContractResponse(contract, 3);

        ProgressTracker progressTracker = ProgressTracker.builder()
                .jobUuid(jobUuid)
                .expectedBeneficiaries(3)
                .failureThreshold(10)
                .build();

        progressTracker.addPatients(patientsByContract);
        JobData jobData = new JobData(contract, progressTracker, job.getSince(),
                getOrganization(job));

        when(jobRepository.findJobStatus(anyString())).thenReturn(JobStatus.CANCELLED);

        var exceptionThrown = assertThrows(JobCancelledException.class,
                () -> cut.process(outputDir, jobData));

        assertTrue(exceptionThrown.getMessage().startsWith("Job was cancelled while it was being processed"));
        verify(patientClaimsProcessor, atLeast(1)).process(any());
        verify(jobRepository, atLeastOnce()).updatePercentageCompleted(anyString(), anyInt());
    }

    @Test
    @DisplayName("When many patientId are present, 'PercentageCompleted' should be updated many times")
    void whenManyPatientIdsAreProcessed_shouldUpdatePercentageCompletedMultipleTimes() throws Exception {
        List<CoverageSummary> patientsByContract = createPatientsByContractResponse(contract, 18);

        ProgressTracker progressTracker = ProgressTracker.builder()
                .jobUuid(jobUuid)
                .expectedBeneficiaries(18)
                .failureThreshold(10)
                .build();
        progressTracker.addPatients(patientsByContract);
        JobData jobData = new JobData(contract, progressTracker, job.getSince(),
                getOrganization(job));

        var jobOutputs = cut.process(outputDir, jobData);

        assertFalse(jobOutputs.isEmpty());
        verify(jobRepository, times(9)).updatePercentageCompleted(anyString(), anyInt());
        verify(patientClaimsProcessor, atLeast(1)).process(any());
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Harry_Potter");
        pdpClient.setEnabled(TRUE);
        pdpClient.setContract(createContract());
        return pdpClient;
    }

    private Contract createContract() {
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_NM_00000");
        contract.setContractNumber("CONTRACT_00000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));

        return contract;
    }

    private Job createJob(PdpClient pdpClient) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatusMessage("0%");
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setPdpClient(pdpClient);
        job.setFhirVersion(STU3);
        return job;
    }

    private static List<CoverageSummary> createPatientsByContractResponse(Contract contract, int num) {
        List<CoverageSummary> summaries = new ArrayList<>();

        FilterOutByDate.DateRange dateRange = TestUtil.getOpenRange();
        for (int i = 0; i < num; i++) {
            CoverageSummary summary = new CoverageSummary(
                    createIdentifierWithoutMbi("patient_" + i),
                    contract,
                    List.of(dateRange)
            );
            summaries.add(summary);
        }
        return summaries;
    }
}