package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import gov.cms.ab2d.worker.util.WorkerDataSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.fhir.IdentifierUtils.BENEFICIARY_ID;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Never run internal coverage processor so this coverage processor runs unimpeded
@SpringBootTest(properties = "coverage.update.initial.delay=1000000")
@Testcontainers
class CoverageUpdateAndProcessorTest {

    private static final int PAST_MONTHS = 3;
    private static final int STALE_DAYS = 3;
    private static final int MAX_ATTEMPTS = 3;
    private static final int STUCK_HOURS = 24;

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Value("${coverage.update.max.attempts}")
    private int maxRetries;

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private ContractWorkerClient contractWorkerClient;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private PdpClientService pdpClientService;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private WorkerDataSetup dataSetup;

    @Autowired
    private CoverageDataSetup coverageDataSetup;

    @Autowired
    private CoverageLockWrapper searchLock;

    @Autowired
    private ContractToContractCoverageMapping mapping;


    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    private Contract contract;
    private CoveragePeriod january;
    private CoveragePeriod february;
    private CoveragePeriod march;

    private BFDClient bfdClient;

    private CoverageDriverImpl driver;
    private CoverageProcessorImpl processor;

    private List<Properties> originalValues = new ArrayList<>();

    @BeforeEach
    void before() {

        // Set values used to find jobs to update
        addPropertiesTableValues();
        originalValues.clear();

        contract = dataSetup.setupContract("TST-12", AB2D_EPOCH.toOffsetDateTime());
        contractRepo.saveAndFlush(contract);

        january = coverageDataSetup.createCoveragePeriod("TST-12", 1, 2020);
        february = coverageDataSetup.createCoveragePeriod("TST-12", 2, 2020);
        march = coverageDataSetup.createCoveragePeriod("TST-12", 3, 2020);

        PdpClientDTO contractPdpClient = createClient(contract.toDTO(), "TST-12", SPONSOR_ROLE);
        pdpClientService.createClient(contractPdpClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-12"));

        bfdClient = mock(BFDClient.class);

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor, MAX_ATTEMPTS, contractWorkerClient);
        driver = new CoverageDriverImpl(coverageSearchRepo, pdpClientService, coverageService, propertiesService, processor, searchLock, mapping);
    }

    @AfterEach
    void cleanup() {
        processor.shutdown();

        dataSetup.cleanup();
        coverageDataSetup.cleanup();

        propertiesService.updateProperties(originalValues.stream().map(properties -> {
            PropertiesDTO dto = new PropertiesDTO();
            dto.setKey(properties.getKey());
            dto.setValue(properties.getValue());
            return dto;
        }).collect(toList()));
    }

    private void addPropertiesTableValues() {
        List<PropertiesDTO> propertiesDTOS = new ArrayList<>();

        PropertiesDTO pastMonths = new PropertiesDTO();
        pastMonths.setKey(Constants.COVERAGE_SEARCH_UPDATE_MONTHS);
        pastMonths.setValue("" + PAST_MONTHS);
        propertiesDTOS.add(pastMonths);

        PropertiesDTO stuckHours = new PropertiesDTO();
        stuckHours.setKey(Constants.COVERAGE_SEARCH_STUCK_HOURS);
        stuckHours.setValue("" + STUCK_HOURS);
        propertiesDTOS.add(stuckHours);

        originalValues.add(propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_UPDATE_MONTHS));
        originalValues.add(propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_STUCK_HOURS));

        propertiesService.updateProperties(propertiesDTOS);
    }

    @DisplayName("Loading coverage periods")
    @Test
    void discoverCoveragePeriods() throws CoverageDriverException, InterruptedException {

        Contract attestedAfterEpoch = dataSetup.setupContract("TST-AFTER-EPOCH",
                AB2D_EPOCH.toOffsetDateTime().plusMonths(3));
        contractRepo.saveAndFlush(attestedAfterEpoch);

        PdpClientDTO attestedAfterClient = createClient(attestedAfterEpoch.toDTO(), "TST-AFTER-EPOCH", SPONSOR_ROLE);
        pdpClientService.createClient(attestedAfterClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-AFTER-EPOCH"));

        Contract attestedBeforeEpoch = dataSetup.setupContract("TST-BEFORE-EPOCH",
                AB2D_EPOCH.toOffsetDateTime().minusNanos(1));
        contractRepo.saveAndFlush(attestedBeforeEpoch);

        PdpClientDTO attestedBeforeClient = createClient(attestedBeforeEpoch.toDTO(), "TST-BEFORE-EPOCH", SPONSOR_ROLE);
        pdpClientService.createClient(attestedBeforeClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-BEFORE-EPOCH"));

        long months = ChronoUnit.MONTHS.between(AB2D_EPOCH.toOffsetDateTime(), OffsetDateTime.now());
        long expectedNumPeriods = months + 1;

        driver.discoverCoveragePeriods();

        List<CoveragePeriod> periods = coveragePeriodRepo.findAllByContractNumber(contract.getContractNumber());
        assertFalse(periods.isEmpty());
        assertEquals(expectedNumPeriods, periods.size());

        periods = coveragePeriodRepo.findAllByContractNumber(attestedAfterEpoch.getContractNumber());
        assertFalse(periods.isEmpty());
        assertEquals(expectedNumPeriods - 3, periods.size());

        periods = coveragePeriodRepo.findAllByContractNumber(attestedBeforeEpoch.getContractNumber());
        assertFalse(periods.isEmpty());
        assertEquals(expectedNumPeriods, periods.size());

    }

    @DisplayName("Cannot submit twice")
    @Test
    void cannotSubmitTwice() {

        coverageService.submitSearch(january.getId(), "testing");

        assertEquals(1, coverageSearchRepo.count());

        processor.queueCoveragePeriod(january, false);

        assertEquals(1, coverageSearchRepo.count());
    }

    @DisplayName("Queue stale coverage find never searched")
    @Test
    void queueStaleCoverageNeverSearched() throws CoverageDriverException, InterruptedException {

        january.setStatus(null);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(null);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        driver.queueStaleCoveragePeriods();

        assertEquals(3, coverageSearchRepo.findAll().size());

        coverageSearchRepo.deleteAll();

        january.setStatus(CoverageJobStatus.SUCCESSFUL);
        january.setLastSuccessfulJob(OffsetDateTime.now());
        coveragePeriodRepo.saveAndFlush(january);

        createEvent(january, CoverageJobStatus.SUCCESSFUL, OffsetDateTime.now());

        february.setStatus(null);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        driver.queueStaleCoveragePeriods();
        assertEquals(2, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverage find never successful")
    @Test
    void queueStaleCoverageNeverSuccessful() throws CoverageDriverException, InterruptedException {

        january.setStatus(CoverageJobStatus.CANCELLED);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(CoverageJobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        createEvent(january, CoverageJobStatus.CANCELLED, OffsetDateTime.now());
        createEvent(february, CoverageJobStatus.FAILED, OffsetDateTime.now());

        driver.queueStaleCoveragePeriods();
        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores coverage periods with last successful search after a boundary in time")
    @Test
    void queueStaleCoverageTimeRanges() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);
        OffsetDateTime previousSunday = currentDate
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .plusSeconds(1);

        OffsetDateTime oneMonthAgo = currentDate.minusMonths(1);
        OffsetDateTime twoMonthsAgo = currentDate.minusMonths(2);

        CoveragePeriod currentMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        currentMonth.setLastSuccessfulJob(previousSunday);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        CoveragePeriod oneMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), oneMonthAgo.getMonthValue(), oneMonthAgo.getYear());
        oneMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        oneMonth.setLastSuccessfulJob(previousSunday);
        coveragePeriodRepo.saveAndFlush(oneMonth);

        CoveragePeriod twoMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), twoMonthsAgo.getMonthValue(), twoMonthsAgo.getYear());
        twoMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        twoMonth.setLastSuccessfulJob(previousSunday);
        coveragePeriodRepo.saveAndFlush(twoMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, previousSunday);
        createEvent(oneMonth, CoverageJobStatus.SUCCESSFUL, previousSunday);
        createEvent(twoMonth, CoverageJobStatus.SUCCESSFUL, previousSunday);

        driver.queueStaleCoveragePeriods();

        assertEquals(0, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores old months")
    @Test
    void queueStaleCoverageIgnoresOldMonths() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);
        OffsetDateTime previousSaturday = currentDate
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .minusSeconds(1);

        OffsetDateTime oneMonthAgo = currentDate.minusMonths(1);
        OffsetDateTime twoMonthsAgo = currentDate.minusMonths(2);
        OffsetDateTime threeMonthsAgo = currentDate.minusMonths(3);

        CoveragePeriod currentMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        currentMonth.setLastSuccessfulJob(previousSaturday);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        CoveragePeriod oneMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), oneMonthAgo.getMonthValue(), oneMonthAgo.getYear());
        oneMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        oneMonth.setLastSuccessfulJob(previousSaturday);
        coveragePeriodRepo.saveAndFlush(oneMonth);

        CoveragePeriod twoMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), twoMonthsAgo.getMonthValue(), twoMonthsAgo.getYear());
        twoMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        twoMonth.setLastSuccessfulJob(previousSaturday);
        coveragePeriodRepo.saveAndFlush(twoMonth);

        CoveragePeriod threeMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), threeMonthsAgo.getMonthValue(), threeMonthsAgo.getYear());
        threeMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
        threeMonth.setLastSuccessfulJob(previousSaturday);
        coveragePeriodRepo.saveAndFlush(threeMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(oneMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(twoMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(threeMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);

        driver.queueStaleCoveragePeriods();

        // Only three because we ignore three months ago
        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages finds jobs stuck beyond a threshold of time")
    @Test
    void queueStaleCoverageFindStuckJobs() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        CoveragePeriod currentMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(CoverageJobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(currentDate.minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, CoverageJobStatus.IN_PROGRESS, currentDate.minusDays(1).minusMinutes(1));

        driver.queueStaleCoveragePeriods();

        assertEquals(1, coverageSearchRepo.findAll().size());

        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == CoverageJobStatus.FAILED));

        assertEquals(CoverageJobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Queue stale coverages ignore coverage periods with non-stuck submitted or in progress jobs")
    @Test
    void queueStaleCoverageIgnoreSubmittedOrInProgress() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        // Test whether queue stale coverage ignores regular in progress jobs

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);
        OffsetDateTime previousSaturday = currentDate
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .minusSeconds(1);

        CoveragePeriod currentMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(CoverageJobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(previousSaturday);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(currentMonth, CoverageJobStatus.IN_PROGRESS, previousSaturday.minusMinutes(1));

        driver.queueStaleCoveragePeriods();

        assertEquals(0, coverageSearchRepo.findAll().size());

        assertEquals(CoverageJobStatus.IN_PROGRESS, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());

        coverageSearchEventRepo.deleteAll();

        // Test whether an already submitted job is queued

        currentMonth.setStatus(CoverageJobStatus.SUBMITTED);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(currentMonth, CoverageJobStatus.SUBMITTED, previousSaturday.minusMinutes(1));

        driver.queueStaleCoveragePeriods();

        assertEquals(0, coverageSearchRepo.findAll().size());
        assertEquals(CoverageJobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Normal workflow functions")
    @Test
    void normalExecution() throws CoverageDriverException, InterruptedException {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20);

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        processor.queueCoveragePeriod(january, false);
        CoverageJobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.SUBMITTED, status);

        driver.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.IN_PROGRESS, status);

        sleep(1000);

        processor.monitorMappingJobs();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.IN_PROGRESS, status);

        processor.insertJobResults();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure leads to retry but still can succeed on retry")
    @Test
    void mappingRetried() {

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt())).thenThrow(new RuntimeException("oops"));

        processor.queueCoveragePeriod(january, false);
        CoverageJobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.SUBMITTED, status);

        driver.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.IN_PROGRESS, status);

        sleep(1000);

        processor.monitorMappingJobs();
        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == CoverageJobStatus.FAILED));

        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.SUBMITTED, status);

        reset(bfdClient);

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20);

        Mockito.clearInvocations();
        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        driver.loadMappingJob();

        sleep(1000);

        processor.monitorMappingJobs();

        sleep(1000);

        processor.insertJobResults();

        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure after x retries")
    @Test
    void mappingFailsAfterXRetries() {

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt())).thenThrow(new RuntimeException("oops"));

        processor.queueCoveragePeriod(january, false);
        CoverageJobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.SUBMITTED, status);

        // Should retry x times
        for (int i = 0; i < maxRetries; i++) {
            status = iterateFailingJob();
            assertEquals(CoverageJobStatus.SUBMITTED, status);
        }

        status = iterateFailingJob();
        assertEquals(CoverageJobStatus.FAILED, status);
    }

    @DisplayName("Only ThreadPoolTaskExecutor.getMaxPoolSize() job results allowed in insertion queue")
    @Test
    void limitRunningJobsByDBSpeed() {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20);

        Mockito.clearInvocations();
        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        ThreadPoolTaskExecutor twoThreads = new ThreadPoolTaskExecutor();
        twoThreads.setMaxPoolSize(2);
        twoThreads.initialize();

        ReflectionTestUtils.setField(processor, "executor", twoThreads);

        processor.queueCoveragePeriod(january, false);
        processor.queueCoveragePeriod(february, false);
        processor.queueCoveragePeriod(march, false);

        driver.loadMappingJob();
        driver.loadMappingJob();

        sleep(1000);

        processor.monitorMappingJobs();

        driver.loadMappingJob();

        assertEquals(0, twoThreads.getActiveCount());
    }

    @DisplayName("Only ThreadPoolTaskExecutor.getCorePoolSize() jobs allowed to run at once")
    @Test
    void limitRunningJobsByThreadPoolSize() {

        ThreadPoolTaskExecutor twoThreads = new ThreadPoolTaskExecutor();
        twoThreads.setCorePoolSize(2);
        twoThreads.setMaxPoolSize(2);
        twoThreads.initialize();

        ReflectionTestUtils.setField(processor, "executor", twoThreads);

        twoThreads.submit(() -> {
           try {
               Thread.sleep(5000);
           } catch (InterruptedException ie) {

           }
        });

        twoThreads.submit(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {

            }
        });

        processor.queueCoveragePeriod(january, false);
        processor.queueCoveragePeriod(february, false);
        processor.queueCoveragePeriod(march, false);

        driver.loadMappingJob();

        sleep(1000);

        assertEquals(2, twoThreads.getActiveCount());
        assertTrue(twoThreads.getThreadPoolExecutor().getQueue().isEmpty());
    }

    @DisplayName("Coverage availability throws exception after max attempts retries")
    @Test
    void coverageAvailabilityLimitsRetries() {

        Job job = new Job();
        job.setCreatedAt(OffsetDateTime.now());
        job.setContractNumber(contract.getContractNumber());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("could not complete test");
        }
        january.setStatus(CoverageJobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(CoverageJobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(february);

        try{
            driver.isCoverageAvailable(job, contract.toDTO());
        }
        catch (CoverageDriverException coverageDriverException) {
            //passed
        }
        catch (InterruptedException interruptedException) {
            fail("could not complete test");
        }

    }

    private CoverageSearchEvent createEvent(CoveragePeriod period, CoverageJobStatus status, OffsetDateTime created) {
        CoverageSearchEvent event = new CoverageSearchEvent();
        event.setCoveragePeriod(period);
        event.setNewStatus(status);
        event.setCreated(created);
        event.setDescription("testing");

        event = coverageSearchEventRepo.saveAndFlush(event);
        event.setCreated(created);
        coverageSearchEventRepo.saveAndFlush(event);

        return event;
    }

    private CoverageJobStatus iterateFailingJob() {
        CoverageJobStatus status;
        driver.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(CoverageJobStatus.IN_PROGRESS, status);

        sleep(1000);

        processor.monitorMappingJobs();
        status = coverageService.getSearchStatus(january.getId());
        return status;
    }

    private org.hl7.fhir.dstu3.model.Bundle buildBundle(int startIndex, int endIndex) {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = new org.hl7.fhir.dstu3.model.Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
            org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();

            org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
            identifier.setSystem(BENEFICIARY_ID);
            identifier.setValue("test-" + i);

            patient.setIdentifier(Collections.singletonList(identifier));
            component.setResource(patient);

            bundle1.addEntry(component);
        }
        return bundle1;
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ie) {

        }
    }


    private PdpClientDTO createClient(ContractDTO contract, String clientId, @Nullable String roleName) {
        PdpClientDTO client = new PdpClientDTO();
        client.setClientId(clientId);
        client.setOrganization(clientId);
        client.setEnabled(true);
        client.setContract(contract);
        client.setRole(roleName);

        return client;
    }
}
