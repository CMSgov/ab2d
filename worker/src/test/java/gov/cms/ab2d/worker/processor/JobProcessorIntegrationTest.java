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

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();
    private Bundle bundle1;
    private RuntimeException fail;
    private List<Resource> bundle1Resources;
    private List<Resource> resources;
    ContractBeneficiaries.PatientDTO patientDTO;

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
        createContract(sponsor);

        ThreadPoolTaskExecutor patientContractThreadPool = new ThreadPoolTaskExecutor();
        patientContractThreadPool.setCorePoolSize(6);
        patientContractThreadPool.setMaxPoolSize(12);
        patientContractThreadPool.setThreadNamePrefix("jobproc-");
        patientContractThreadPool.initialize();

        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundle1Resources = bundle1.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        resources = getResources();
        future = mock(Future.class);

        patientDTO = new ContractBeneficiaries.PatientDTO();
        patientDTO.setPatientId("-199900000022040");
        patientDTO.setDateRangesUnderContract(Collections.singletonList(
                new FilterOutByDate.DateRange(new Date(0), new Date())));

        ContractBeneficiaries.PatientDTO patientDTO2 = new ContractBeneficiaries.PatientDTO();
        patientDTO2.setPatientId("-199900000022041");
        patientDTO2.setDateRangesUnderContract(Collections.singletonList(
                new FilterOutByDate.DateRange(new Date(0), new Date())));

        Mockito.lenient().when(patientClaimsProcessor.process(any())).thenReturn(future);
        Mockito.lenient().when(future.get()).thenReturn(new EobSearchResponse(patientDTO, bundle1Resources));
        Mockito.lenient().when(future.isDone()).thenReturn(true);

        when(mockBfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(getNumPatients(
                new String[] {
                        "-199900000022040", "-199900000022041", "-199900000022042", "-199900000022043", "-199900000022044", "-199900000022045", "-199900000022046", "-199900000022047", "-199900000022048", "-199900000022049",
                        "-199900000022050", "-199900000022051", "-199900000022052", "-199900000022053", "-199900000022054", "-199900000022055", "-199900000022056", "-199900000022057", "-199900000022058", "-199900000022059",
                        "-199900000022060", "-199900000022061", "-199900000022062", "-199900000022063", "-199900000022064", "-199900000022065", "-199900000022066", "-199900000022067", "-199900000022068", "-199900000022069",
                        "-199900000022070", "-199900000022071", "-199900000022072", "-199900000022073", "-199900000022074", "-199900000022047", "-199900000022076", "-199900000022077", "-199900000022078", "-199900000022079",
                        "-199900000022080", "-199900000022081", "-199900000022082", "-199900000022083", "-199900000022084", "-199900000022085", "-199900000022086", "-199900000022087", "-199900000022088", "-199900000022089",
                        "-199900000022090", "-199900000022091", "-199900000022092", "-199900000022093", "-199900000022094", "-199900000022095", "-199900000022096", "-199900000022097", "-199900000022098", "-199900000022099",
                        "-199900000022000", "-199900000022001", "-199900000022002", "-199900000022003", "-199900000022004", "-199900000022005", "-199900000022006", "-199900000022007", "-199900000022008", "-199900000022009",
                        "-199900000022010", "-199900000022011", "-199900000022012", "-199900000022013", "-199900000022014", "-199900000022015", "-199900000022016", "-199900000022017", "-199900000022018", "-199900000022019",
                        "-199900000022020", "-199900000022021", "-199900000022022", "-199900000022023", "-199900000022024", "-199900000022025", "-199900000022026", "-199900000022027", "-199900000022028", "-199900000022029",
                        "-199900000022030", "-199900000022031", "-199900000022032", "-199900000022033", "-199900000022034", "-199900000022035", "-199900000022036", "-199900000022037", "-199900000022038", "-199900000022039"
                }
        ));

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
        ReflectionTestUtils.setField(cut, "startDateSpecialContracts",
                "01/01/1900");
        ReflectionTestUtils.setField(cut, "specialContracts", Collections.singletonList("Z0001"));

        ReflectionTestUtils.setField(cut, "efsMount", tmpEfsMountDir.toString());
        ReflectionTestUtils.setField(cut, "failureThreshold", 10);
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
    void processJob() {
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
    void whenJobSubmittedByParentUser_ProcessAllContractsForChildrenSponsors() {
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

    @Test
    @DisplayName("When the error count is below threshold, job does not fail")
    void when_errorCount_is_below_threshold_do_not_fail_job() throws ExecutionException, InterruptedException {
        EobSearchResponse response = new EobSearchResponse(patientDTO, resources);
        Mockito.when(future.get())
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response)
                .thenThrow(fail, fail, fail, fail, fail)
        ;

        var processedJob = cut.process("S0000");

        assertEquals(processedJob.getStatus(), JobStatus.SUCCESSFUL);
        assertEquals(processedJob.getStatusMessage(), "100%");
        assertNotNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());

        List<LoggableEvent> beneSearchEvents = doAll.load(ContractBeneSearchEvent.class);
        assertEquals(1, beneSearchEvents.size());
        ContractBeneSearchEvent event = (ContractBeneSearchEvent) beneSearchEvents.get(0);
        assertEquals("S0000", event.getJobId());
        assertEquals(100, event.getNumInContract());
        assertEquals("CONTRACT_0000", event.getContractNumber());
        assertEquals(100, event.getNumSearched());

        final List<JobOutput> jobOutputs = job.getJobOutputs();
        assertFalse(jobOutputs.isEmpty());
    }

    @Test
    @DisplayName("When the error count is greater than or equal to threshold, job should fail")
    void when_errorCount_is_not_below_threshold_fail_job() throws ExecutionException, InterruptedException {
        EobSearchResponse response = new EobSearchResponse(patientDTO, resources);
        Mockito.when(future.get())
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenReturn(response, response, response, response, response, response, response, response, response, response)
                .thenThrow(fail, fail, fail, fail, fail, fail, fail, fail, fail, fail)
        ;
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
        assertEquals(40, fileEvents.size());
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.OPEN).count());
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getStatus() == FileEvent.FileStatus.CLOSE).count());
        assertTrue(((FileEvent) fileEvents.get(39)).getFileName().contains("0020.ndjson"));
        assertTrue(((FileEvent) fileEvents.get(0)).getFileName().contains("0001.ndjson"));
        assertEquals(20, fileEvents.stream().filter(e -> ((FileEvent) e).getFileHash().length() > 0).count());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class)));

        assertEquals(processedJob.getStatus(), JobStatus.FAILED);
        assertEquals(processedJob.getStatusMessage(), "Too many patient records in the job had failures");
        assertNull(processedJob.getExpiresAt());
        assertNotNull(processedJob.getCompletedAt());
    }

    private List<Resource> getResources() {
        List<Resource> allResources = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            allResources.addAll(bundle1Resources);
        }
        return allResources;
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

    private void createContract(Sponsor sponsor) {
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_0000");
        contract.setContractNumber("CONTRACT_0000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        contractRepository.save(contract);
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