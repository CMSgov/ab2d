package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Never run internal coverage processor so this coverage processor runs unimpeded
@SpringBootTest(properties = "coverage.update.initial.delay=1000000")
@Testcontainers
class CoverageDriverTest {

    private static final int PAST_MONTHS = 3;
    private static final int STALE_DAYS = 3;
    private static final int MAX_ATTEMPTS = 3;
    private static final int STUCK_HOURS = 24;

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private ContractService contractService;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private JobRepository jobRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private PdpClientService pdpClientService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private CoverageDataSetup coverageDataSetup;

    @Autowired
    private CoverageLockWrapper searchLock;

    @Autowired
    private ContractToContractCoverageMapping contractToContractCoverageMapping;

    private Contract contract;
    private Contract contract1;
    private ContractForCoverageDTO contractForCoverageDTO;
    private ContractForCoverageDTO contractForCoverageDTO1;
    private CoveragePeriod january;
    private CoveragePeriod february;
    private CoveragePeriod march;
    private Job job;

    private BFDClient bfdClient;

    private CoverageDriverImpl driver;
    private CoverageProcessorImpl processor;

    @BeforeEach
    void before() {

        // Set properties values in database
        addPropertiesTableValues();

        contract = dataSetup.setupContract("TST-12", AB2D_EPOCH.toOffsetDateTime());

        contract1 = dataSetup.setupContract("TST-45", AB2D_EPOCH.toOffsetDateTime());

        contractForCoverageDTO = new ContractForCoverageDTO("TST-12", AB2D_EPOCH.toOffsetDateTime(),ContractForCoverageDTO.ContractType.NORMAL);
        contractForCoverageDTO1 = new ContractForCoverageDTO("TST-45", AB2D_EPOCH.toOffsetDateTime(),ContractForCoverageDTO.ContractType.NORMAL);


        contractRepo.saveAndFlush(contract);

        january = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 1, 2020);
        february = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 2, 2020);
        march = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 3, 2020);

        PdpClientDTO contractPdpClient = createClient(contract, "TST-12", SPONSOR_ROLE);
        pdpClientService.createClient(contractPdpClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-12"));

        PdpClient pdpClient = dataSetup.setupPdpClient(List.of());
        job = new Job();
        job.setContractNumber(contract.getContractNumber());
        job.setJobUuid("unique");
        job.setOrganization(pdpClient.getOrganization());
        job.setStatus(gov.cms.ab2d.common.model.JobStatus.SUBMITTED);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        jobRepo.saveAndFlush(job);
        dataSetup.queueForCleanup(job);

        bfdClient = mock(BFDClient.class);

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor, MAX_ATTEMPTS, contractService);
        driver = new CoverageDriverImpl(coverageSearchRepo, pdpClientService, coverageService, propertiesService, processor, searchLock, contractToContractCoverageMapping);
    }

    @AfterEach
    void cleanup() {
        processor.shutdown();

        coverageDataSetup.cleanup();
        dataSetup.cleanup();

        PropertiesDTO engagement = new PropertiesDTO();
        engagement.setKey(Constants.WORKER_ENGAGEMENT);
        engagement.setValue(FeatureEngagement.IN_GEAR.getSerialValue());

        PropertiesDTO override = new PropertiesDTO();
        override.setKey(Constants.COVERAGE_SEARCH_OVERRIDE);
        override.setValue("false");
        propertiesService.updateProperties(List.of(engagement, override));
    }

    private void addPropertiesTableValues() {
        List<PropertiesDTO> propertiesDTOS = new ArrayList<>();

        PropertiesDTO workerEngagement = new PropertiesDTO();
        workerEngagement.setKey(Constants.WORKER_ENGAGEMENT);
        workerEngagement.setValue(FeatureEngagement.NEUTRAL.getSerialValue());
        propertiesDTOS.add(workerEngagement);

        PropertiesDTO pastMonths = new PropertiesDTO();
        pastMonths.setKey(Constants.COVERAGE_SEARCH_UPDATE_MONTHS);
        pastMonths.setValue("" + PAST_MONTHS);
        propertiesDTOS.add(pastMonths);

        PropertiesDTO stuckHours = new PropertiesDTO();
        stuckHours.setKey(Constants.COVERAGE_SEARCH_STUCK_HOURS);
        stuckHours.setValue("" + STUCK_HOURS);
        propertiesDTOS.add(stuckHours);

        propertiesService.updateProperties(propertiesDTOS);
    }

    @DisplayName("Loading coverage periods")
    @Test
    void discoverCoveragePeriods() {

        Contract attestedAfterEpoch = dataSetup.setupContract("TST-AFTER-EPOCH",
                AB2D_EPOCH.toOffsetDateTime().plusMonths(3));
        contractRepo.saveAndFlush(attestedAfterEpoch);

        PdpClientDTO attestedAfterClient = createClient(attestedAfterEpoch, "TST-AFTER-EPOCH", SPONSOR_ROLE);
        pdpClientService.createClient(attestedAfterClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-AFTER-EPOCH"));

        Contract attestedBeforeEpoch = dataSetup.setupContract("TST-BEFORE-EPOCH",
                AB2D_EPOCH.toOffsetDateTime().minusNanos(1));
        contractRepo.saveAndFlush(attestedBeforeEpoch);

        PdpClientDTO attestedBeforeClient = createClient(attestedBeforeEpoch, "TST-BEFORE-EPOCH", SPONSOR_ROLE);
        pdpClientService.createClient(attestedBeforeClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-BEFORE-EPOCH"));

        long months = ChronoUnit.MONTHS.between(AB2D_EPOCH.toOffsetDateTime(), OffsetDateTime.now());
        long expectedNumPeriods = months + 1;

        try {
            driver.discoverCoveragePeriods();
        } catch (CoverageDriverException | InterruptedException exception) {
            fail("could not queue periods due to driver exception", exception);
        }

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

    @DisplayName("Ignore contracts marked test")
    @Test
    void discoverCoveragePeriodsIgnoresTestContracts() {

        Contract testContract = dataSetup.setupContract("TST-AFTER-EPOCH",
                AB2D_EPOCH.toOffsetDateTime().plusMonths(3));
        testContract.setUpdateMode(Contract.UpdateMode.NONE);

        contractRepo.saveAndFlush(testContract);

        try {
            driver.discoverCoveragePeriods();
        } catch (CoverageDriverException | InterruptedException exception) {
            fail("could not queue periods due to driver exception", exception);
        }
        List<CoveragePeriod> periods = coveragePeriodRepo.findAllByContractNumber(testContract.getContractNumber());
        assertTrue(periods.isEmpty());
    }

    @DisplayName("Queue stale coverage find never searched")
    @Test
    void queueStaleCoverageNeverSearched() {

        january.setStatus(null);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(null);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

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

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(2, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverage find never successful")
    @Test
    void queueStaleCoverageNeverSuccessful() {

        january.setStatus(CoverageJobStatus.CANCELLED);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(CoverageJobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        createEvent(january, CoverageJobStatus.CANCELLED, OffsetDateTime.now());
        createEvent(february, CoverageJobStatus.FAILED, OffsetDateTime.now());

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores coverage periods with last successful search after a boundary in time")
    @Test
    void queueStaleCoverageTimeRanges() {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);
        OffsetDateTime previousSunday = currentDate.truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusSeconds(1);

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

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(0, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores coverage periods with last successful search before a boundary in time")
    @Test
    void queueStaleCoverageOverrideRecentlySearched() {

        coveragePeriodRepo.deleteAll();

        PropertiesDTO override = new PropertiesDTO();
        override.setKey(Constants.COVERAGE_SEARCH_OVERRIDE);
        override.setValue("true");
        propertiesService.updateProperties(singletonList(override));

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);
        OffsetDateTime previousSunday = currentDate
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previous(DayOfWeek.SUNDAY)).plusSeconds(1);

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

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores coverage periods belonging to old months")
    @Test
    void queueStaleCoverageIgnoresOldMonths() {

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

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        // Only three because we ignore three months ago
        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages finds coverage periods that got stuck in progress")
    @Test
    void queueStaleCoverageFindStuckJobs() {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        CoveragePeriod currentMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(CoverageJobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(currentDate.minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, CoverageJobStatus.IN_PROGRESS, currentDate.minusDays(1).minusMinutes(1));

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(1, coverageSearchRepo.findAll().size());

        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == CoverageJobStatus.FAILED));

        assertEquals(CoverageJobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Queue stale coverages ignore coverage periods with non-stuck submitted or in progress jobs")
    @Test
    void queueStaleCoverageIgnoreSubmittedOrInProgress() {

        coveragePeriodRepo.deleteAll();

        // Test whether queue stale coverage ignores regular in progress jobs

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);
        OffsetDateTime previousSaturday = currentDate
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previous(DayOfWeek.SUNDAY)).minusSeconds(1);

        CoveragePeriod currentMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(CoverageJobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(previousSaturday);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(currentMonth, CoverageJobStatus.IN_PROGRESS, previousSaturday.minusMinutes(1));

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(0, coverageSearchRepo.findAll().size());

        assertEquals(CoverageJobStatus.IN_PROGRESS, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());

        coverageSearchEventRepo.deleteAll();

        // Test whether an already submitted job is queued

        currentMonth.setStatus(CoverageJobStatus.SUBMITTED);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, CoverageJobStatus.SUCCESSFUL, previousSaturday);
        createEvent(currentMonth, CoverageJobStatus.SUBMITTED, previousSaturday.minusMinutes(1));

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(0, coverageSearchRepo.findAll().size());
        assertEquals(CoverageJobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Normal workflow functions")
    @Test
    void normalExecution() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

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

    /**
     * Verify that null is returned if there are no searches, a search there is one and verify that it
     * was deleted after it was searched.
     */
    @DisplayName("Getting another search gets and removes a coverage search specification")
    @Test
    void getNextSearchDefaultsToFirst() {
        assertTrue(driver.getNextSearch().isEmpty());

        CoverageSearch search1 = new CoverageSearch(null, january, OffsetDateTime.now(), 0);
        CoverageSearch savedSearch1 = coverageSearchRepo.save(search1);
        Optional<CoverageSearch> returnedSearch = driver.getNextSearch();

        assertEquals(savedSearch1.getPeriod().getMonth(), returnedSearch.get().getPeriod().getMonth());
        assertEquals(savedSearch1.getPeriod().getYear(), returnedSearch.get().getPeriod().getYear());
        assertTrue(driver.getNextSearch().isEmpty());
    }

    /**
     * Verify that null is returned if there are no searches, a search there is one and verify that it
     * was deleted after it was searched.
     */
    @DisplayName("Getting a search prioritizes coverage searches for already submitted eob jobs")
    @Test
    void getNextSearchPrioritizesCoverageForExistinEobJobs() {

        CoveragePeriod secondPeriod = coverageDataSetup.createCoveragePeriod(contract1.getContractName(), 2, 2020);

        assertTrue(driver.getNextSearch().isEmpty());

        coverageService.submitSearch(secondPeriod.getId(), "first submitted");
        coverageService.submitSearch(january.getId(), "second submitted");

        Optional<CoverageSearch> coverageSearch = driver.getNextSearch();
        assertTrue(coverageSearch.isPresent());
        assertEquals(january, coverageSearch.get().getPeriod());

        coverageSearch = driver.getNextSearch();
        assertTrue(coverageSearch.isPresent());
        assertEquals(secondPeriod, coverageSearch.get().getPeriod());

        assertTrue(driver.getNextSearch().isEmpty());
    }

    @DisplayName("Do not start an eob job if any relevant coverage period has never had data pulled for it")
    @Test
    void availableCoverageWhenNeverSearched() {

        Job job = new Job();
        job.setContractNumber(contract.getContractNumber());

        try {
            boolean noCoverageStatuses = driver.isCoverageAvailable(job, contract);

            assertFalse(noCoverageStatuses, "eob searches should not run when a" +
                    " coverage period has no information");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }

    @DisplayName("Do not start an eob job if any relevant coverage period is queued for an update")
    @Test
    void availableCoverageWhenPeriodSubmitted() {

        Job job = new Job();
        job.setContractNumber(contractForCoverageDTO.getContractNumber());
        job.setCreatedAt(OffsetDateTime.now());

        try {
            changeStatus(contractForCoverageDTO, AB2D_EPOCH.toOffsetDateTime(), CoverageJobStatus.SUBMITTED);

            // Make sure that there is a lastSuccessfulJob
            ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
            CoveragePeriod currentMonth = coverageService.getCoveragePeriod(contractForCoverageDTO, now.getMonthValue(), now.getYear());
            currentMonth.setLastSuccessfulJob(OffsetDateTime.now().plusHours(2));
            currentMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(currentMonth);

            boolean submittedCoverageStatus = driver.isCoverageAvailable(job, contract);
            assertFalse(submittedCoverageStatus, "eob searches should not run if a " +
                    "coverage period is submitted");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }

    @DisplayName("Do not start an eob job if any relevant coverage period is being updated")
    @Test
    void availableCoverageWhenPeriodInProgress() {

        Job job = new Job();
        job.setContractNumber(contract.getContractNumber());
        job.setCreatedAt(OffsetDateTime.now());

        try {

            changeStatus(contractForCoverageDTO, AB2D_EPOCH.toOffsetDateTime(), CoverageJobStatus.IN_PROGRESS);

            // Make sure that there is a lastSuccessfulJob
            ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
            CoveragePeriod currentMonth = coverageService.getCoveragePeriod(contractForCoverageDTO, now.getMonthValue(), now.getYear());
            currentMonth.setLastSuccessfulJob(OffsetDateTime.now().plusHours(2));
            currentMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(currentMonth);

            boolean inProgressCoverageStatus = driver.isCoverageAvailable(job, contract);
            assertFalse(inProgressCoverageStatus, "eob searches should not run when a coverage period is in progress");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }

    @DisplayName("Do start an eob job if all coverage periods are in progress")
    @Test
    void availableCoverageWhenAllSuccessful() {

        Job job = new Job();
        job.setContractNumber(contractForCoverageDTO.getContractNumber());
        job.setCreatedAt(OffsetDateTime.now());

        try {

            changeStatus(contractForCoverageDTO, AB2D_EPOCH.toOffsetDateTime(), CoverageJobStatus.SUCCESSFUL);

            // Make sure that there is a lastSuccessfulJob
            ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
            CoveragePeriod currentMonth = coverageService.getCoveragePeriod(contractForCoverageDTO, now.getMonthValue(), now.getYear());
            currentMonth.setLastSuccessfulJob(OffsetDateTime.now().plusHours(2));
            currentMonth.setStatus(CoverageJobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(currentMonth);

            boolean submittedCoverageStatus = driver.isCoverageAvailable(job, contract);
            assertTrue(submittedCoverageStatus, "eob searches should not run if a " +
                    "coverage period is submitted");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }

    /**
     * The since date is only relevant for claims data not enrollment data. So even though the since date
     * is set the enrollment data must all be up to date before a job can start.
     */
    @DisplayName("Do not start an eob job if periods before since are being worked on. Ignore since.")
    @Test
    void availableCoverageWhenSinceContainsOnlySuccessful() {

        Job job = new Job();
        job.setCreatedAt(OffsetDateTime.now());

        Contract temp = contractRepo.findContractByContractNumber(contractForCoverageDTO.getContractNumber()).get();
        job.setContractNumber(temp.getContractNumber());

        OffsetDateTime since = OffsetDateTime.of(LocalDate.of(2020, 3, 1),
                LocalTime.of(0, 0, 0), AB2D_ZONE.getRules().getOffset(Instant.now()));

        try {

            changeStatus(contractForCoverageDTO, since, CoverageJobStatus.SUCCESSFUL);

            LocalDate startMonth = LocalDate.of(2020, 3, 1);
            LocalTime startDay = LocalTime.of(0,0,0);

            job.setSince(OffsetDateTime.of(startMonth, startDay, AB2D_ZONE.getRules().getOffset(Instant.now())));

            boolean inProgressBeginningMonth = driver.isCoverageAvailable(job, contract);
            assertFalse(inProgressBeginningMonth, "eob searches should run when only month after since is successful");

            LocalDate endMonth = LocalDate.of(2020, 3, 31);
            LocalTime endDay = LocalTime.of(23,59,59);

            job.setSince(OffsetDateTime.of(endMonth, endDay, AB2D_ZONE.getRules().getOffset(Instant.now())));

            boolean inProgressEndMonth = driver.isCoverageAvailable(job, contract);
            assertFalse(inProgressEndMonth, "eob searches should run when only month after since is successful");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }


    @DisplayName("Number of beneficiaries to process calculation works")
    @Test
    void numberOfBeneficiariesToProcess() {

        // Override BeforeEach method settings to make this test work for a smaller period of time
        contract.setAttestedOn(OffsetDateTime.now().minus(1, ChronoUnit.SECONDS));
        contractRepo.save(contract);

        CoveragePeriod period = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), contract.getESTAttestationTime().getMonthValue(), contract.getESTAttestationTime().getYear());

        int total = driver.numberOfBeneficiariesToProcess(job, contract);
        assertEquals(0, total);

        CoverageSearchEvent event = new CoverageSearchEvent();
        event.setOldStatus(CoverageJobStatus.SUBMITTED);
        event.setNewStatus(CoverageJobStatus.IN_PROGRESS);
        event.setDescription("test");
        event.setCoveragePeriod(period);
        event = coverageSearchEventRepo.saveAndFlush(event);
        dataSetup.queueForCleanup(event);

        Set<Identifiers> members = new HashSet<>();
        members.add(new Identifiers(1, "1234", new LinkedHashSet<>()));
        coverageService.insertCoverage(event.getId(), members);

        total = driver.numberOfBeneficiariesToProcess(job, contract);
        assertEquals(1, total);
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

    private org.hl7.fhir.dstu3.model.Bundle buildBundle(int startIndex, int endIndex) {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = new org.hl7.fhir.dstu3.model.Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
            org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();

            org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
            identifier.setSystem(IdentifierUtils.BENEFICIARY_ID);
            identifier.setValue("test-" + i);

            patient.setIdentifier(singletonList(identifier));
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

    private void changeStatus(ContractForCoverageDTO contract, OffsetDateTime attestationTime, CoverageJobStatus status) {

        OffsetDateTime now = OffsetDateTime.now();
        while (attestationTime.isBefore(now)) {
            CoveragePeriod period = coverageService.getCreateIfAbsentCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());

            period.setStatus(status);
            if (status == CoverageJobStatus.SUCCESSFUL) {
                period.setLastSuccessfulJob(now);
            }
            coveragePeriodRepo.saveAndFlush(period);

            attestationTime = attestationTime.plusMonths(1);
        }
    }

    private PdpClientDTO createClient(Contract contract, String clientId, @Nullable String roleName) {
        PdpClientDTO client = new PdpClientDTO();
        client.setClientId(clientId);
        client.setOrganization(clientId);
        client.setEnabled(true);
        ContractDTO contractDTO = new ContractDTO(contract.getContractNumber(), contract.getContractName(),
                contract.getAttestedOn().toString());
        client.setContract(contractDTO);
        client.setRole(roleName);

        return client;
    }
}
