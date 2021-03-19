package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.common.util.DateUtil;
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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.fhir.IdentifierUtils.BENEFICIARY_ID;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
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
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private ContractService contractService;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private CoverageLockWrapper searchLock;

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

        contract = dataSetup.setupContract("TST-123");
        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());
        contractRepo.saveAndFlush(contract);

        january = dataSetup.createCoveragePeriod(contract, 1, 2020);
        february = dataSetup.createCoveragePeriod(contract, 2, 2020);
        march = dataSetup.createCoveragePeriod(contract, 3, 2020);

        bfdClient = mock(BFDClient.class);

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor, MAX_ATTEMPTS, false);
        driver = new CoverageDriverImpl(coverageSearchRepo, contractService, coverageService, propertiesService, processor, searchLock);
    }

    @AfterEach
    void cleanup() {
        processor.shutdown();

        dataSetup.cleanup();

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

        PropertiesDTO staleDays = new PropertiesDTO();
        staleDays.setKey(Constants.COVERAGE_SEARCH_STALE_DAYS);
        staleDays.setValue("" + STALE_DAYS);
        propertiesDTOS.add(staleDays);

        PropertiesDTO stuckHours = new PropertiesDTO();
        stuckHours.setKey(Constants.COVERAGE_SEARCH_STUCK_HOURS);
        stuckHours.setValue("" + STUCK_HOURS);
        propertiesDTOS.add(stuckHours);

        originalValues.add(propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_UPDATE_MONTHS));
        originalValues.add(propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_STALE_DAYS));
        originalValues.add(propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_STUCK_HOURS));

        propertiesService.updateProperties(propertiesDTOS);
    }

    @DisplayName("Loading coverage periods")
    @Test
    void discoverCoveragePeriods() throws CoverageDriverException, InterruptedException {

        Contract attestedAfterEpoch = dataSetup.setupContract("TST-AFTER-EPOCH");
        attestedAfterEpoch.setAttestedOn(AB2D_EPOCH.toOffsetDateTime().plusMonths(3));
        contractRepo.saveAndFlush(attestedAfterEpoch);

        Contract attestedBeforeEpoch = dataSetup.setupContract("TST-BEFORE-EPOCH");
        attestedBeforeEpoch.setAttestedOn(AB2D_EPOCH.toOffsetDateTime().minusNanos(1));
        contractRepo.saveAndFlush(attestedBeforeEpoch);

        long months = ChronoUnit.MONTHS.between(AB2D_EPOCH.toOffsetDateTime(), OffsetDateTime.now());
        long expectedNumPeriods = months + 1;

        driver.discoverCoveragePeriods();

        List<CoveragePeriod> periods = coveragePeriodRepo.findAllByContractId(contract.getId());
        assertFalse(periods.isEmpty());
        assertEquals(expectedNumPeriods, periods.size());

        periods = coveragePeriodRepo.findAllByContractId(attestedAfterEpoch.getId());
        assertFalse(periods.isEmpty());
        assertEquals(expectedNumPeriods - 3, periods.size());

        periods = coveragePeriodRepo.findAllByContractId(attestedBeforeEpoch.getId());
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

        january.setStatus(JobStatus.SUCCESSFUL);
        january.setLastSuccessfulJob(OffsetDateTime.now());
        coveragePeriodRepo.saveAndFlush(january);

        createEvent(january, JobStatus.SUCCESSFUL, OffsetDateTime.now());

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

        january.setStatus(JobStatus.CANCELLED);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(JobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        createEvent(january, JobStatus.CANCELLED, OffsetDateTime.now());
        createEvent(february, JobStatus.FAILED, OffsetDateTime.now());

        driver.queueStaleCoveragePeriods();
        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores coverage periods with last successful search after a boundary in time")
    @Test
    void queueStaleCoverageTimeRanges() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        OffsetDateTime oneMonthAgo = currentDate.minusMonths(1);
        OffsetDateTime twoMonthsAgo = currentDate.minusMonths(2);

        CoveragePeriod currentMonth = dataSetup.createCoveragePeriod(contract, currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(JobStatus.SUCCESSFUL);
        currentMonth.setLastSuccessfulJob(currentDate.minusDays(STALE_DAYS - 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        CoveragePeriod oneMonth = dataSetup.createCoveragePeriod(contract, oneMonthAgo.getMonthValue(), oneMonthAgo.getYear());
        oneMonth.setStatus(JobStatus.SUCCESSFUL);
        oneMonth.setLastSuccessfulJob(currentDate.minusDays(2 * STALE_DAYS - 1));
        coveragePeriodRepo.saveAndFlush(oneMonth);

        CoveragePeriod twoMonth = dataSetup.createCoveragePeriod(contract, twoMonthsAgo.getMonthValue(), twoMonthsAgo.getYear());
        twoMonth.setStatus(JobStatus.SUCCESSFUL);
        twoMonth.setLastSuccessfulJob(currentDate.minusDays(3 * STALE_DAYS - 1));
        coveragePeriodRepo.saveAndFlush(twoMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS - 1));
        createEvent(oneMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(2 * STALE_DAYS - 1));
        createEvent(twoMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(3 * STALE_DAYS - 1));

        driver.queueStaleCoveragePeriods();

        assertEquals(0, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages finds coverage periods whose last successful search is before a boundary in time")
    @Test
    void queueStaleCoverageIgnoresOldMonths() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        OffsetDateTime oneMonthAgo = currentDate.minusMonths(1);
        OffsetDateTime twoMonthsAgo = currentDate.minusMonths(2);
        OffsetDateTime threeMonthsAgo = currentDate.minusMonths(3);

        CoveragePeriod currentMonth = dataSetup.createCoveragePeriod(contract, currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(JobStatus.SUCCESSFUL);
        currentMonth.setLastSuccessfulJob(currentDate.minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        CoveragePeriod oneMonth = dataSetup.createCoveragePeriod(contract, oneMonthAgo.getMonthValue(), oneMonthAgo.getYear());
        oneMonth.setStatus(JobStatus.SUCCESSFUL);
        oneMonth.setLastSuccessfulJob(currentDate.minusDays(2 * STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(oneMonth);

        CoveragePeriod twoMonth = dataSetup.createCoveragePeriod(contract, twoMonthsAgo.getMonthValue(), twoMonthsAgo.getYear());
        twoMonth.setStatus(JobStatus.SUCCESSFUL);
        twoMonth.setLastSuccessfulJob(currentDate.minusDays(3 * STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(twoMonth);

        CoveragePeriod threeMonth = dataSetup.createCoveragePeriod(contract, threeMonthsAgo.getMonthValue(), threeMonthsAgo.getYear());
        threeMonth.setStatus(JobStatus.SUCCESSFUL);
        threeMonth.setLastSuccessfulJob(currentDate.minusDays(4 * STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(threeMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(oneMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(2 * STALE_DAYS + 1));
        createEvent(twoMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(3 * STALE_DAYS + 1));
        createEvent(threeMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(4 * STALE_DAYS + 1));

        driver.queueStaleCoveragePeriods();

        // Only three because we ignore three months ago
        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages finds coverage periods whose last successful search before a boundary")
    @Test
    void queueStaleCoverageFindStuckJobs() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        CoveragePeriod currentMonth = dataSetup.createCoveragePeriod(contract, currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(JobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(currentDate.minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, JobStatus.IN_PROGRESS, currentDate.minusDays(1).minusMinutes(1));

        driver.queueStaleCoveragePeriods();

        assertEquals(1, coverageSearchRepo.findAll().size());

        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == JobStatus.FAILED));

        assertEquals(JobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Queue stale coverages ignore coverage periods with non-stuck submitted or in progress jobs")
    @Test
    void queueStaleCoverageIgnoreSubmittedOrInProgress() throws CoverageDriverException, InterruptedException {

        coveragePeriodRepo.deleteAll();

        // Test whether queue stale coverage ignores regular in progress jobs

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        CoveragePeriod currentMonth = dataSetup.createCoveragePeriod(contract, currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(JobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(OffsetDateTime.now().minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, JobStatus.IN_PROGRESS, currentDate.minusMinutes(1));

        driver.queueStaleCoveragePeriods();

        assertEquals(0, coverageSearchRepo.findAll().size());

        assertEquals(JobStatus.IN_PROGRESS, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());

        coverageSearchEventRepo.deleteAll();

        // Test whether an already submitted job is queued

        currentMonth.setStatus(JobStatus.SUBMITTED);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, JobStatus.SUBMITTED, currentDate.minusMinutes(1));

        driver.queueStaleCoveragePeriods();

        assertEquals(0, coverageSearchRepo.findAll().size());
        assertEquals(JobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
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
        JobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        driver.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        sleep(1000);

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

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt())).thenThrow(new RuntimeException("oops"));

        processor.queueCoveragePeriod(january, false);
        JobStatus status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        driver.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        sleep(1000);

        processor.monitorMappingJobs();
        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == JobStatus.FAILED));

        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.SUBMITTED, status);

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
        assertEquals(JobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure after x retries")
    @Test
    void mappingFailsAfterXRetries() {

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt())).thenThrow(new RuntimeException("oops"));

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

    private CoverageSearchEvent createEvent(CoveragePeriod period, JobStatus status, OffsetDateTime created) {
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

    private JobStatus iterateFailingJob() {
        JobStatus status;
        driver.loadMappingJob();
        status = coverageService.getSearchStatus(january.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

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
}
