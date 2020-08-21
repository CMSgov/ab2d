package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResponse;
import gov.cms.ab2d.worker.service.FileService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.transaction.Transactional;
import java.io.File;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
@Transactional
class JobProcessorIntegrationTest {
    private final Random random = new Random();

    private JobProcessor cut;       // class under test

    @Autowired
    private FileService fileService;
    @Autowired
    private DoAll doAll;
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
    private SqlEventLogger sqlEventLogger;
    @Mock
    private KinesisEventLogger kinesisEventLogger;
    @Mock
    private BFDClient mockBfdClient;
    @Mock
    private PatientClaimsProcessorImpl patientClaimsProcessor;

    @TempDir
    File tmpEfsMountDir;

    private Sponsor sponsor;
    private User user;
    private Job job;
    private Future<EobSearchResponse> future;
    private String[] testBenes = new String[] {
            "-199900000022040", "-199900000022041", "-199900000022042", "-199900000022043", "-199900000022044", "-199900000022045", "-199900000022046", "-199900000022047", "-199900000022048", "-199900000022049",
            "-199900000022050", "-199900000022051", "-199900000022052", "-199900000022053", "-199900000022054", "-199900000022055", "-199900000022056", "-199900000022057", "-199900000022058", "-199900000022059",
            "-199900000022060", "-199900000022061", "-199900000022062", "-199900000022063", "-199900000022064", "-199900000022065", "-199900000022066", "-199900000022067", "-199900000022068", "-199900000022069",
            "-199900000022070", "-199900000022071", "-199900000022072", "-199900000022073", "-199900000022074", "-199900000022075", "-199900000022076", "-199900000022077", "-199900000022078", "-199900000022079",
            "-199900000022030", "-199900000022031", "-199900000022032", "-199900000022033", "-199900000022034", "-199900000022035", "-199900000022036", "-199900000022037", "-199900000022038", "-199900000022039"
    };

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();
    private Bundle bundle1;
    private RuntimeException fail;
    private List<Resource> bundle1Resources;
    private ContractBeneficiaries.PatientDTO patientDTO;
    private Contract contract;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException, NoSuchFieldException, ParseException {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();
        doAll.delete();

        LogManager logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        sponsor = createSponsor();
        user = createUser(sponsor);
        job = createJob(user);

        job.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.save(job);
        contract = createContract(sponsor);

        ThreadPoolTaskExecutor patientContractThreadPool = new ThreadPoolTaskExecutor();
        patientContractThreadPool.setCorePoolSize(6);
        patientContractThreadPool.setMaxPoolSize(12);
        patientContractThreadPool.setThreadNamePrefix("jobproc-");
        patientContractThreadPool.initialize();

        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundle1Resources = bundle1.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        future = mock(Future.class);

        ContractBeneficiaries.PatientDTO patientDTO2 = new ContractBeneficiaries.PatientDTO();
        patientDTO2.setPatientId("-199900000022041");
        patientDTO2.setDateRangesUnderContract(Collections.singletonList(
                new FilterOutByDate.DateRange(new Date(0), new Date())));

        Mockito.lenient().when(patientClaimsProcessor.process(any())).thenReturn(future);
        Mockito.lenient().when(future.get()).thenReturn(new EobSearchResponse(patientDTO, bundle1Resources));
        Mockito.lenient().when(future.isDone()).thenReturn(true);

        when(mockBfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(getNumPatients(testBenes));

        fail = new RuntimeException("TEST EXCEPTION");

        FhirContext fhirContext = FhirContext.forDstu3();
        ReflectionTestUtils.setField(patientClaimsProcessor, "startDate", "01/01/1900");
        cut = new JobProcessorImpl(
                fileService,
                jobRepository,
                jobOutputRepository,
                logManager,
                mockBfdClient,
                patientClaimsProcessor,
                fhirContext,
                patientContractThreadPool
        );
        ReflectionTestUtils.setField(cut, "startDate", "01/01/1990");
        ReflectionTestUtils.setField(cut, "startDateSpecialContracts", "01/01/1900");
        ReflectionTestUtils.setField(cut, "specialContracts", Collections.singletonList("Z0001"));
        ReflectionTestUtils.setField(cut, "efsMount", tmpEfsMountDir.toString());
        ReflectionTestUtils.setField(cut, "failureThreshold", 10);
        ReflectionTestUtils.setField(cut, "ndjsonRollOver", 200);
    }

    Bundle getNumPatients(String[] patients) {
        Bundle bundlePatient = new Bundle();
        for (int i = 0; i < patients.length; i++) {
            Bundle.BundleEntryComponent entry = BundleUtils.createBundleEntry(patients[i]);
            bundlePatient.addEntry(entry);
        }
        return bundlePatient;
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be processed")
    void processJob() throws ExecutionException, InterruptedException {
        Mockito.when(future.get())
                .thenReturn(
                        getResources(testBenes[0]), getResources(testBenes[1]), getResources(testBenes[2]), getResources(testBenes[3]), getResources(testBenes[4]),
                        getResources(testBenes[5]), getResources(testBenes[6]), getResources(testBenes[7]), getResources(testBenes[8]), getResources(testBenes[9]),
                        getResources(testBenes[10]), getResources(testBenes[11]), getResources(testBenes[12]), getResources(testBenes[13]), getResources(testBenes[14]),
                        getResources(testBenes[15]), getResources(testBenes[16]), getResources(testBenes[17]), getResources(testBenes[18]), getResources(testBenes[19]),
                        getResources(testBenes[20]), getResources(testBenes[21]), getResources(testBenes[22]), getResources(testBenes[23]), getResources(testBenes[24]),
                        getResources(testBenes[25]), getResources(testBenes[26]), getResources(testBenes[27]), getResources(testBenes[28]), getResources(testBenes[29]),
                        getResources(testBenes[30]), getResources(testBenes[31]), getResources(testBenes[32]), getResources(testBenes[33]), getResources(testBenes[34]),
                        getResources(testBenes[35]), getResources(testBenes[36]), getResources(testBenes[37]), getResources(testBenes[38]), getResources(testBenes[39]),
                        getResources(testBenes[40]), getResources(testBenes[41]), getResources(testBenes[42]), getResources(testBenes[43]), getResources(testBenes[44]),
                        getResources(testBenes[45]), getResources(testBenes[46]), getResources(testBenes[47]), getResources(testBenes[48]), getResources(testBenes[49]));
        var processedJob = cut.process("S0000");

        List<LoggableEvent> jobStatusChange = doAll.load(JobStatusChangeEvent.class);
        Assert.assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobStatusChange.get(0);
        Assert.assertEquals(JobStatus.SUCCESSFUL.name(), jobEvent.getNewStatus());
        Assert.assertEquals(JobStatus.IN_PROGRESS.name(), jobEvent.getOldStatus());

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    @DisplayName("When a job is in submitted by the parent user, it process the contracts for the children")
    void whenJobSubmittedByParentUser_ProcessAllContractsForChildrenSponsors() throws ExecutionException, InterruptedException {
        Mockito.when(future.get())
                .thenReturn(
                        getResources(testBenes[0]), getResources(testBenes[1]), getResources(testBenes[2]), getResources(testBenes[3]), getResources(testBenes[4]),
                        getResources(testBenes[5]), getResources(testBenes[6]), getResources(testBenes[7]), getResources(testBenes[8]), getResources(testBenes[9]),
                        getResources(testBenes[10]), getResources(testBenes[11]), getResources(testBenes[12]), getResources(testBenes[13]), getResources(testBenes[14]),
                        getResources(testBenes[15]), getResources(testBenes[16]), getResources(testBenes[17]), getResources(testBenes[18]), getResources(testBenes[19]),
                        getResources(testBenes[20]), getResources(testBenes[21]), getResources(testBenes[22]), getResources(testBenes[23]), getResources(testBenes[24]),
                        getResources(testBenes[25]), getResources(testBenes[26]), getResources(testBenes[27]), getResources(testBenes[28]), getResources(testBenes[29]),
                        getResources(testBenes[30]), getResources(testBenes[31]), getResources(testBenes[32]), getResources(testBenes[33]), getResources(testBenes[34]),
                        getResources(testBenes[35]), getResources(testBenes[36]), getResources(testBenes[37]), getResources(testBenes[38]), getResources(testBenes[39]),
                        getResources(testBenes[40]), getResources(testBenes[41]), getResources(testBenes[42]), getResources(testBenes[43]), getResources(testBenes[44]),
                        getResources(testBenes[45]), getResources(testBenes[46]), getResources(testBenes[47]), getResources(testBenes[48]), getResources(testBenes[49]));

        // switch the user to the parent sponsor
        user.setSponsor(sponsor.getParent());
        userRepository.save(user);

        var processedJob = cut.process("S0000");

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    private ContractBeneficiaries.PatientDTO getPatient(String id) {
        ContractBeneficiaries.PatientDTO patient = new ContractBeneficiaries.PatientDTO();
        patient.setPatientId(id);
        return patient;
    }

    @Test
    @DisplayName("When the error count is below threshold, job does not fail")
    void when_errorCount_is_below_threshold_do_not_fail_job() throws ExecutionException, InterruptedException {
        Mockito.when(future.get())
                .thenReturn(
                        getResources(testBenes[0]), getResources(testBenes[1]), getResources(testBenes[2]), getResources(testBenes[3]), getResources(testBenes[4]),
                        getResources(testBenes[5]), getResources(testBenes[6]), getResources(testBenes[7]), getResources(testBenes[8]), getResources(testBenes[9]),
                        getResources(testBenes[10]), getResources(testBenes[11]), getResources(testBenes[12]), getResources(testBenes[13]), getResources(testBenes[14]),
                        getResources(testBenes[15]), getResources(testBenes[16]), getResources(testBenes[17]), getResources(testBenes[18]), getResources(testBenes[19]),
                        getResources(testBenes[20]), getResources(testBenes[21]), getResources(testBenes[22]), getResources(testBenes[23]), getResources(testBenes[24]),
                        getResources(testBenes[25]), getResources(testBenes[26]), getResources(testBenes[27]), getResources(testBenes[28]), getResources(testBenes[29]),
                        getResources(testBenes[30]), getResources(testBenes[31]), getResources(testBenes[32]), getResources(testBenes[33]), getResources(testBenes[34]),
                        getResources(testBenes[35]), getResources(testBenes[36]), getResources(testBenes[37]), getResources(testBenes[38]), getResources(testBenes[39]),
                        getResources(testBenes[40]), getResources(testBenes[41]), getResources(testBenes[42]), getResources(testBenes[43]), getResources(testBenes[44]),
                        getResources(testBenes[45]))
                .thenThrow(fail, fail, fail, fail);

        var processedJob = cut.process("S0000");

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        List<LoggableEvent> beneSearchEvents = doAll.load(ContractBeneSearchEvent.class);
        assertEquals(1, beneSearchEvents.size());
        ContractBeneSearchEvent event = (ContractBeneSearchEvent) beneSearchEvents.get(0);
        assertEquals("S0000", event.getJobId());
        assertEquals(50, event.getNumInContract());
        assertEquals("CONTRACT_0000", event.getContractNumber());
        assertEquals(50, event.getNumSearched());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    @DisplayName("When the error count is greater than or equal to threshold, job should fail")
    void when_errorCount_is_not_below_threshold_fail_job() throws ExecutionException, InterruptedException {
        Mockito.when(future.get())
                .thenReturn(getResources(testBenes[0]), getResources(testBenes[1]), getResources(testBenes[2]), getResources(testBenes[3]), getResources(testBenes[4]),
                        getResources(testBenes[5]), getResources(testBenes[6]), getResources(testBenes[7]), getResources(testBenes[8]), getResources(testBenes[9]),
                        getResources(testBenes[10]), getResources(testBenes[11]), getResources(testBenes[12]), getResources(testBenes[13]), getResources(testBenes[14]),
                        getResources(testBenes[15]), getResources(testBenes[16]), getResources(testBenes[17]), getResources(testBenes[18]), getResources(testBenes[19]))
                .thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail)
                .thenReturn(getResources(testBenes[30]), getResources(testBenes[31]), getResources(testBenes[32]), getResources(testBenes[33]), getResources(testBenes[34]),
                        getResources(testBenes[35]), getResources(testBenes[36]), getResources(testBenes[37]), getResources(testBenes[38]), getResources(testBenes[39]))
                .thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail);
        var processedJob = cut.process("S0000");

        List<LoggableEvent> errorEvents = doAll.load(ErrorEvent.class);
        assertEquals(1, errorEvents.size());
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(TOO_MANY_SEARCH_ERRORS, errorEvent.getErrorType());

        List<LoggableEvent> jobEvents = doAll.load(JobStatusChangeEvent.class);
        assertEquals(1, jobEvents.size());
        JobStatusChangeEvent jobEvent = (JobStatusChangeEvent) jobEvents.get(0);
        assertEquals("IN_PROGRESS", jobEvent.getOldStatus());
        assertEquals("FAILED", jobEvent.getNewStatus());

        List<LoggableEvent> fileEvents = doAll.load(FileEvent.class);
        // Since the max size of the file is not set here (so it's 0), every second write creates a new file since
        // the file is no longer empty after the first write. This means, there were 20 files created so 40 events
        assertEquals(2, fileEvents.size());
        assertEquals(1, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.OPEN).count());
        assertEquals(1, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.CLOSE).count());
        assertTrue(((FileEvent) fileEvents.get(0)).getFileName().contains("0001.ndjson"));
        assertEquals(1, fileEvents.stream().filter(e -> ((FileEvent) e).getFileHash().length() > 0).count());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ContractBeneSearchEvent.class)));

        assertEquals(processedJob.getStatus(), JobStatus.FAILED);
        assertEquals(processedJob.getStatusMessage(), "Too many patient records in the job had failures");
        assertNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());
    }

    private EobSearchResponse getResources(String id) {
        List<Resource> allResources = new ArrayList<>();
        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        try {
            Date d1 = new Date(new Date().getTime() - 10000000);
            Date d2 = new Date(new Date().getTime() - 8000000);
            Period period = new Period();
            period.setStart(d1);
            period.setEnd(d2);
            eob.setBillablePeriod(period);
            eob.getPatient().setReference("Patient/" + id);
            for (int i = 0; i < 8; i++) {
                allResources.add(eob.copy());
            }
        } catch (Exception ex) {
            fail(ex);
        }
        return new EobSearchResponse(getPatient(id), allResources);
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
        contractRepository.save(contract);
        return contract;
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setUser(user);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }
}