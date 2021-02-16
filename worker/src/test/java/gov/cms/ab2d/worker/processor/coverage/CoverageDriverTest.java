package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.fhir.Versions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.ab2d.common.util.DateUtil.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private JobRepository jobRepo;

    @Autowired
    private UserRepository userRepo;

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
    private Contract contract1;
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

        contract = dataSetup.setupContract("TST-123");
        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());

        contract1 = dataSetup.setupContract("TST-456");
        contract1.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());

        contractRepo.saveAndFlush(contract);

        january = dataSetup.createCoveragePeriod(contract, 1, 2020);
        february = dataSetup.createCoveragePeriod(contract, 2, 2020);
        march = dataSetup.createCoveragePeriod(contract, 3, 2020);

        User user = dataSetup.setupUser(List.of());
        job = new Job();
        job.setContract(contract);
        job.setJobUuid("unique");
        job.setUser(user);
        job.setStatus(JobStatus.SUBMITTED);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(Versions.FhirVersions.STU3);
        jobRepo.saveAndFlush(job);
        dataSetup.queueForCleanup(job);

        bfdClient = mock(BFDClient.class);

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();

        CoverageUpdateConfig config = new CoverageUpdateConfig(PAST_MONTHS, STALE_DAYS, STUCK_HOURS);

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor, MAX_ATTEMPTS, false);
        driver = new CoverageDriverImpl(coverageSearchRepo, contractService, coverageService, propertiesService, processor, searchLock);
    }

    @AfterEach
    void cleanup() {
        processor.shutdown();

        dataSetup.cleanup();

        PropertiesDTO dto = new PropertiesDTO();
        dto.setKey(Constants.WORKER_ENGAGEMENT);
        dto.setValue(FeatureEngagement.IN_GEAR.getSerialValue());
        propertiesService.updateProperties(singletonList(dto));
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

        PropertiesDTO staleDays = new PropertiesDTO();
        staleDays.setKey(Constants.COVERAGE_SEARCH_STALE_DAYS);
        staleDays.setValue("" + STALE_DAYS);
        propertiesDTOS.add(staleDays);

        PropertiesDTO stuckHours = new PropertiesDTO();
        stuckHours.setKey(Constants.COVERAGE_SEARCH_STUCK_HOURS);
        stuckHours.setValue("" + STUCK_HOURS);
        propertiesDTOS.add(stuckHours);

        propertiesService.updateProperties(propertiesDTOS);
    }

    @DisplayName("Loading coverage periods")
    @Test
    void discoverCoveragePeriods() {

        Contract attestedAfterEpoch = dataSetup.setupContract("TST-AFTER-EPOCH");
        attestedAfterEpoch.setAttestedOn(AB2D_EPOCH.toOffsetDateTime().plusMonths(3));
        contractRepo.saveAndFlush(attestedAfterEpoch);

        Contract attestedBeforeEpoch = dataSetup.setupContract("TST-BEFORE-EPOCH");
        attestedBeforeEpoch.setAttestedOn(AB2D_EPOCH.toOffsetDateTime().minusNanos(1));
        contractRepo.saveAndFlush(attestedBeforeEpoch);

        long months = ChronoUnit.MONTHS.between(AB2D_EPOCH.toOffsetDateTime(), OffsetDateTime.now());
        long expectedNumPeriods = months + 1;

        try {
            driver.discoverCoveragePeriods();
        } catch (CoverageDriverException | InterruptedException exception) {
            fail("could not queue periods due to driver exception", exception);
        }

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

    @DisplayName("Ignore contracts marked test")
    @Test
    void discoverCoveragePeriodsIgnoresTestContracts() {

        Contract testContract = dataSetup.setupContract("TST-AFTER-EPOCH");
        testContract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime().plusMonths(3));
        testContract.setUpdateMode(Contract.UpdateMode.TEST);

        contractRepo.saveAndFlush(testContract);

        try {
            driver.discoverCoveragePeriods();
        } catch (CoverageDriverException | InterruptedException exception) {
            fail("could not queue periods due to driver exception", exception);
        }
        List<CoveragePeriod> periods = coveragePeriodRepo.findAllByContractId(testContract.getId());
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

        january.setStatus(JobStatus.SUCCESSFUL);
        january.setLastSuccessfulJob(OffsetDateTime.now());
        coveragePeriodRepo.saveAndFlush(january);

        createEvent(january, JobStatus.SUCCESSFUL, OffsetDateTime.now());

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

        january.setStatus(JobStatus.CANCELLED);
        coveragePeriodRepo.saveAndFlush(january);

        february.setStatus(JobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(february);

        march.setStatus(null);
        coveragePeriodRepo.saveAndFlush(march);

        createEvent(january, JobStatus.CANCELLED, OffsetDateTime.now());
        createEvent(february, JobStatus.FAILED, OffsetDateTime.now());

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages ignores coverage periods with last successful search after a boundary in time")
    @Test
    void queueStaleCoverageTimeRanges() {

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

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(0, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages finds coverage periods whose last successful search is before a boundary in time")
    @Test
    void queueStaleCoverageIgnoresOldMonths() {

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

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        // Only three because we ignore three months ago
        assertEquals(3, coverageSearchRepo.findAll().size());
    }

    @DisplayName("Queue stale coverages finds coverage periods whose last successful search before a boundary")
    @Test
    void queueStaleCoverageFindStuckJobs() {

        coveragePeriodRepo.deleteAll();

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        CoveragePeriod currentMonth = dataSetup.createCoveragePeriod(contract, currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(JobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(currentDate.minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, JobStatus.IN_PROGRESS, currentDate.minusDays(1).minusMinutes(1));

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(1, coverageSearchRepo.findAll().size());

        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == JobStatus.FAILED));

        assertEquals(JobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Queue stale coverages ignore coverage periods with non-stuck submitted or in progress jobs")
    @Test
    void queueStaleCoverageIgnoreSubmittedOrInProgress() {

        coveragePeriodRepo.deleteAll();

        // Test whether queue stale coverage ignores regular in progress jobs

        OffsetDateTime currentDate = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        CoveragePeriod currentMonth = dataSetup.createCoveragePeriod(contract, currentDate.getMonthValue(), currentDate.getYear());
        currentMonth.setStatus(JobStatus.IN_PROGRESS);
        currentMonth.setLastSuccessfulJob(OffsetDateTime.now().minusDays(STALE_DAYS + 1));
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, JobStatus.IN_PROGRESS, currentDate.minusMinutes(1));

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(0, coverageSearchRepo.findAll().size());

        assertEquals(JobStatus.IN_PROGRESS, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());

        coverageSearchEventRepo.deleteAll();

        // Test whether an already submitted job is queued

        currentMonth.setStatus(JobStatus.SUBMITTED);
        coveragePeriodRepo.saveAndFlush(currentMonth);

        createEvent(currentMonth, JobStatus.SUCCESSFUL, currentDate.minusDays(STALE_DAYS + 1));
        createEvent(currentMonth, JobStatus.SUBMITTED, currentDate.minusMinutes(1));

        assertDoesNotThrow(() -> driver.queueStaleCoveragePeriods(), "could not queue periods due to driver exception");

        assertEquals(0, coverageSearchRepo.findAll().size());
        assertEquals(JobStatus.SUBMITTED, coveragePeriodRepo.findById(currentMonth.getId()).get().getStatus());
    }

    @DisplayName("Normal workflow functions")
    @Test
    void normalExecution() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);
        when(bfdClient.getVersion()).thenReturn(Versions.FhirVersions.STU3);

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

        CoveragePeriod secondPeriod = dataSetup.createCoveragePeriod(contract1, 2, 2020);

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
        job.setContract(contract);

        try {
            boolean noCoverageStatuses = driver.isCoverageAvailable(job);

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
        job.setContract(contract);
        job.setCreatedAt(OffsetDateTime.now());

        try {
            changeStatus(contract, AB2D_EPOCH.toOffsetDateTime(), JobStatus.SUBMITTED);

            // Make sure that there is a lastSuccessfulJob
            ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
            CoveragePeriod currentMonth = coverageService.getCoveragePeriod(contract, now.getMonthValue(), now.getYear());
            currentMonth.setLastSuccessfulJob(OffsetDateTime.now().plusHours(2));
            currentMonth.setStatus(JobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(currentMonth);

            boolean submittedCoverageStatus = driver.isCoverageAvailable(job);
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
        job.setContract(contract);
        job.setCreatedAt(OffsetDateTime.now());

        try {

            changeStatus(contract, AB2D_EPOCH.toOffsetDateTime(), JobStatus.IN_PROGRESS);

            // Make sure that there is a lastSuccessfulJob
            ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
            CoveragePeriod currentMonth = coverageService.getCoveragePeriod(contract, now.getMonthValue(), now.getYear());
            currentMonth.setLastSuccessfulJob(OffsetDateTime.now().plusHours(2));
            currentMonth.setStatus(JobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(currentMonth);

            boolean inProgressCoverageStatus = driver.isCoverageAvailable(job);
            assertFalse(inProgressCoverageStatus, "eob searches should not run when a coverage period is in progress");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }

    @DisplayName("Do start an eob job if all coverage periods are in progress")
    @Test
    void availableCoverageWhenAllSuccessful() {

        Job job = new Job();
        job.setContract(contract);
        job.setCreatedAt(OffsetDateTime.now());

        try {

            changeStatus(contract, AB2D_EPOCH.toOffsetDateTime(), JobStatus.SUCCESSFUL);

            // Make sure that there is a lastSuccessfulJob
            ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
            CoveragePeriod currentMonth = coverageService.getCoveragePeriod(contract, now.getMonthValue(), now.getYear());
            currentMonth.setLastSuccessfulJob(OffsetDateTime.now().plusHours(2));
            currentMonth.setStatus(JobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(currentMonth);

            boolean submittedCoverageStatus = driver.isCoverageAvailable(job);
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

        Contract temp = contractRepo.findContractByContractNumber(contract.getContractNumber()).get();
        job.setContract(temp);

        OffsetDateTime since = OffsetDateTime.of(LocalDate.of(2020, 3, 1),
                LocalTime.of(0, 0, 0), AB2D_ZONE.getRules().getOffset(Instant.now()));

        try {

            changeStatus(contract, since, JobStatus.SUCCESSFUL);

            LocalDate startMonth = LocalDate.of(2020, 3, 1);
            LocalTime startDay = LocalTime.of(0,0,0);

            job.setSince(OffsetDateTime.of(startMonth, startDay, AB2D_ZONE.getRules().getOffset(Instant.now())));

            boolean inProgressBeginningMonth = driver.isCoverageAvailable(job);
            assertFalse(inProgressBeginningMonth, "eob searches should run when only month after since is successful");

            LocalDate endMonth = LocalDate.of(2020, 3, 31);
            LocalTime endDay = LocalTime.of(23,59,59);

            job.setSince(OffsetDateTime.of(endMonth, endDay, AB2D_ZONE.getRules().getOffset(Instant.now())));

            boolean inProgressEndMonth = driver.isCoverageAvailable(job);
            assertFalse(inProgressEndMonth, "eob searches should run when only month after since is successful");
        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
    }

    @DisplayName("Create a coverage period and a mapping job for an eob job if any periods do not exist or have never" +
            " been searched")
    @Test
    void availableCoverageDiscoversCoveragePeriodsAndQueuesThem() {

        Job job = new Job();
        job.setContract(contract);

        long numberPeriodsBeforeCheck = coveragePeriodRepo.count();

        try {

            boolean inProgressBeginningMonth = driver.isCoverageAvailable(job);
            assertFalse(inProgressBeginningMonth, "eob searches should run when only month after since is successful");

            assertTrue(numberPeriodsBeforeCheck < coveragePeriodRepo.count());

            Set<CoveragePeriod> periods = contract.getCoveragePeriods();
            periods.forEach(period -> assertEquals(JobStatus.SUBMITTED, period.getStatus()));

        } catch (InterruptedException | CoverageDriverException exception) {
            fail("could not check for available coverage", exception);
        }
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

    private void changeStatus(Contract contract, OffsetDateTime attestationTime, JobStatus status) {

        OffsetDateTime now = OffsetDateTime.now();
        while (attestationTime.isBefore(now)) {
            CoveragePeriod period = coverageService.getCreateIfAbsentCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());

            period.setStatus(status);
            if (status == JobStatus.SUCCESSFUL) {
                period.setLastSuccessfulJob(now);
            }
            coveragePeriodRepo.saveAndFlush(period);

            attestationTime = attestationTime.plusMonths(1);
        }
    }
}
