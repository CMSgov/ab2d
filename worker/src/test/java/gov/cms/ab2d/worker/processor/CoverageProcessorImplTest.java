package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.worker.config.CoverageMappingConfig;
import gov.cms.ab2d.worker.processor.domainmodel.ContractSearchLock;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

import static gov.cms.ab2d.worker.processor.CoverageMappingCallable.BENEFICIARY_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class CoverageProcessorImplTest {

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Value("${coverage.update.max.attempts}")
    private int maxRetries;

    @Autowired
    private SponsorRepository sponsorRepo;

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoverageRepository coverageRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private ContractSearchLock searchLock;

    private Sponsor sponsor;
    private Contract contract;
    private CoveragePeriod january;
    private CoveragePeriod february;
    private CoveragePeriod march;

    private BFDClient bfdClient;

    private ThreadPoolTaskExecutor taskExecutor;
    private CoverageProcessorImpl processor;

    @BeforeEach
    void before() {

        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract = dataSetup.setupContract(sponsor, "TST-123");

        january = dataSetup.createCoveragePeriod(contract, 1, 2020);
        february = dataSetup.createCoveragePeriod(contract, 2, 2020);
        march = dataSetup.createCoveragePeriod(contract, 3, 2020);

        bfdClient = mock(BFDClient.class);

        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();

        CoverageMappingConfig config = new CoverageMappingConfig(3, 3, 3, 24);

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor, config, searchLock);
    }

    @AfterEach
    void after() {
        processor.shutdown();

        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
        contractRepo.delete(contract);
        contractRepo.flush();

        if (sponsor != null) {
            sponsorRepo.delete(sponsor);
            sponsorRepo.flush();
        }
    }

    @DisplayName("Cannot submit twice")
    @Test
    void cannotSubmitTwice() {

        coverageService.submitSearch(january.getId(), "testing");

        assertEquals(1, coverageSearchRepo.count());

        processor.queueCoveragePeriod(january, false);

        assertEquals(1, coverageSearchRepo.count());
    }

    @DisplayName("Normal workflow functions")
    @Test
    void normalExecution() {

        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        processor.queueCoveragePeriod(january, false);
        JobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        processor.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        processor.monitorMappingJobs();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        processor.insertJobResults();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure leads to retry but still can succeed on retry")
    @Test
    void mappingRetried() {

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenThrow(new RuntimeException("oops"));

        processor.queueCoveragePeriod(january, false);
        JobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        processor.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        processor.monitorMappingJobs();
        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == JobStatus.FAILED));

        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        reset(bfdClient);

        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        Mockito.clearInvocations();
        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        processor.loadMappingJob();

        sleep();

        processor.monitorMappingJobs();

        sleep();

        processor.insertJobResults();

        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure after x retries")
    @Test
    void mappingFailsAfterXRetries() {

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenThrow(new RuntimeException("oops"));


        processor.queueCoveragePeriod(january, false);
        JobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        // Should retry x times
        for (int i = 0; i < maxRetries; i++) {
            status = iterateFailingJob();
            assertEquals(JobStatus.SUBMITTED, status);
        }

        status = iterateFailingJob();
        assertEquals(JobStatus.FAILED, status);
    }

    @DisplayName("Only ThreadPoolTaskExecutor.getMaxPoolSize() jobs started at a time")
    @Test
    void limitRunningJobsByCallableSpeed() {
        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        Mockito.clearInvocations();
        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        ThreadPoolTaskExecutor singleThread = new ThreadPoolTaskExecutor();
        singleThread.setMaxPoolSize(1);
        singleThread.initialize();

        ReflectionTestUtils.setField(processor, "executor", singleThread);

        processor.queueCoveragePeriod(january, false);
        processor.queueCoveragePeriod(february, false);

        processor.loadMappingJob();
        processor.loadMappingJob();

        assertEquals(1, singleThread.getActiveCount());

    }

    @DisplayName("Only ThreadPoolTaskExecutor.getMaxPoolSize() job results allowed in insertion queue")
    @Test
    void limitRunningJobsByDBSpeed() {
        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        Mockito.clearInvocations();
        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        ThreadPoolTaskExecutor twoThreads = new ThreadPoolTaskExecutor();
        twoThreads.setMaxPoolSize(2);
        twoThreads.initialize();

        ReflectionTestUtils.setField(processor, "executor", twoThreads);

        processor.queueCoveragePeriod(january, false);
        processor.queueCoveragePeriod(february, false);
        processor.queueCoveragePeriod(march, false);

        processor.loadMappingJob();
        processor.loadMappingJob();
        sleep();
        processor.monitorMappingJobs();

        processor.loadMappingJob();

        assertEquals(0, twoThreads.getActiveCount());
    }

    private JobStatus iterateFailingJob() {
        JobStatus status;
        processor.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        sleep();

        processor.monitorMappingJobs();
        status = coverageService.getSearchStatus(january.getId());
        return status;
    }

    private Bundle buildBundle(int startIndex, int endIndex) {
        Bundle bundle1 = new Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            Patient patient = new Patient();

            Identifier identifier = new Identifier();
            identifier.setSystem(BENEFICIARY_ID);
            identifier.setValue("test-" + i);

            patient.setIdentifier(Collections.singletonList(identifier));
            component.setResource(patient);

            bundle1.addEntry(component);
        }
        return bundle1;
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {

        }
    }
}
