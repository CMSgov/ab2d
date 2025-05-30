package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoverageDelta;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchDiff;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoverageDeltaRepository;
import gov.cms.ab2d.coverage.repository.CoverageDeltaTestRepository;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.coverage.util.AB2DCoverageLocalstackContainer;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;
import gov.cms.ab2d.coverage.util.Coverage;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.filter.FilterOutByDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import javax.sql.DataSource;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.coverage.repository.CoverageDeltaRepository.COVERAGE_ADDED;
import static gov.cms.ab2d.coverage.repository.CoverageDeltaRepository.COVERAGE_DELETED;
import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SuppressWarnings("OptionalGetWithoutIsPresent")
@SpringBootTest
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.coverage.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.coverage.repository"})
@Testcontainers
@TestPropertySource(locations = "/application.coverage.properties")
@EnableFeignClients(clients = {ContractFeignClient.class})
class CoverageServiceImplTest {
    private static final int YEAR = 2020;
    private static final int JANUARY = 1;
    private static final int FEBRUARY = 2;
    private static final int MARCH = 3;
    private static final int APRIL = 4;
    private static final int MAY = 5;

    // Used to test the coverage summary code

    private static final Date START_JAN = FilterOutByDate.getStartOfMonth(1, 2020);
    private static final Date START_FEB = FilterOutByDate.getStartOfMonth(2, 2020);
    private static final Date START_MARCH = FilterOutByDate.getStartOfMonth(3, 2020);
    private static final Date START_APRIL = FilterOutByDate.getStartOfMonth(4, 2020);
    private static final Date START_MAY = FilterOutByDate.getStartOfMonth(5, 2020);

    private static final Date END_DEC = FilterOutByDate.getEndOfMonth(12, 2019);
    private static final Date END_JAN = FilterOutByDate.getEndOfMonth(1, 2020);
    private static final Date END_FEB = FilterOutByDate.getEndOfMonth(2, 2020);
    private static final Date END_MARCH = FilterOutByDate.getEndOfMonth(3, 2020);
    private static final Date END_APRIL = FilterOutByDate.getEndOfMonth(4, 2020);

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DCoveragePostgressqlContainer();

    @Container
    private static final AB2DCoverageLocalstackContainer localstackContainer = new AB2DCoverageLocalstackContainer();

    @Autowired
    CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    CoverageSearchRepository coverageSearchRepo;

    @Autowired
    CoverageDeltaRepository coverageDeltaRepository;

    @Autowired
    CoverageDeltaTestRepository coverageDeltaTestRepository;

    @Autowired
    CoverageService coverageService;

    @MockitoSpyBean
    SQSEventClient eventLogger;

    @Autowired
    CoverageDataSetup dataSetup;

    @Autowired
    DataSource dataSource;

    private final PropertiesService propertiesService = new PropertyServiceStub();

    private CoverageServiceRepository  coverageServiceRepo;

    private ContractForCoverageDTO contract1;
    private ContractForCoverageDTO contract2;

    private CoveragePeriod period1Jan;
    private CoveragePeriod period1Feb;
    private CoveragePeriod period1March;
    private CoveragePeriod period1April;
    private OffsetDateTime jobStartTime;

    private CoveragePeriod period2Jan;
    @Autowired
    private ApplicationContext context;

    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {
        CoverageServiceRepository c = context.getBean(CoverageServiceRepository.class);
        ReflectionTestUtils.setField(c, "propertiesService", propertiesService);
        contract1 = dataSetup.setupContractDTO("TST-12", AB2D_EPOCH.toOffsetDateTime());
        contract2 = dataSetup.setupContractDTO("TST-34", AB2D_EPOCH.toOffsetDateTime());
        period1Jan = dataSetup.createCoveragePeriod("TST-12", JANUARY, YEAR);
        period1Feb = dataSetup.createCoveragePeriod("TST-12", FEBRUARY, YEAR);
        period1March = dataSetup.createCoveragePeriod("TST-12", MARCH, YEAR);
        period1April = dataSetup.createCoveragePeriod("TST-12", APRIL, YEAR);
        jobStartTime = OffsetDateTime.of(YEAR, APRIL, 2, 0, 0, 0, 0, ZoneOffset.UTC);

        period2Jan = dataSetup.createCoveragePeriod("TST-34", JANUARY, YEAR);

        coverageServiceRepo = new CoverageServiceRepository(dataSource, coveragePeriodRepo, coverageSearchEventRepo, propertiesService);
        propertiesService.createProperty("OptOutOn", "false");
    }

    @AfterEach
    public void cleanUp() {
        dataSetup.cleanup();
    }

    @Test
    void testFindAssociatedCoveragePeriods() {
        List<CoveragePeriod> coveragePeriodsOne = coverageService.findAssociatedCoveragePeriods("TST-12");
        assertFalse(coveragePeriodsOne.isEmpty());

        List<CoveragePeriod> coveragePeriodsTwo = coverageService.findAssociatedCoveragePeriods("does-not-exist");
        assertTrue(coveragePeriodsTwo.isEmpty());
    }

    @DisplayName("Get a coverage period")
    @Test
    void getCoveragePeriod() {
        CoveragePeriod period = coverageService.getCoveragePeriod(contract1, JANUARY, YEAR);
        assertEquals(period1Jan, period);

    }

    @DisplayName("Get a coverage period fails on EntityNotFoundException")
    @Test
    void getCoveragePeriodFailsOnEntityException() {

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> coverageService.getCoveragePeriod(contract1, 12, 2020));

        assertEquals("could not find coverage period matching contract, month, and year", exception.getMessage());
    }

    @DisplayName("Get or create does not insert duplicate period")
    @Test
    void getOrCreateDoesNotDuplicate() {

        long periods = coveragePeriodRepo.count();

        coverageService.getCreateIfAbsentCoveragePeriod(contract1, JANUARY, YEAR);

        assertEquals(periods, coveragePeriodRepo.count());
    }

    @DisplayName("Get or create does not insert duplicate period")
    @Test
    void getOrCreateInsertsNew() {
        CoveragePeriod period = coverageService.getCreateIfAbsentCoveragePeriod(contract1, 12, 2020);
        coveragePeriodRepo.delete(period);

        assertThrows(EntityNotFoundException.class,
            () -> coverageService.getCoveragePeriod(contract1, 12, 2020)
        );
    }

    @DisplayName("Coverage search status works")
    @Test
    void checkCoveragePeriodStatus() {

        coverageService.submitSearch(period1Jan.getId(), "testing");

        assertTrue(coverageService.canEOBSearchBeStarted(period1Jan.getId()));
        assertFalse(coverageService.isCoveragePeriodInProgress(period1Jan.getId()));

        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageService.startSearch(search.get(), "testing");

        assertFalse(coverageService.canEOBSearchBeStarted(period1Jan.getId()));
        assertTrue(coverageService.isCoveragePeriodInProgress(period1Jan.getId()));

        coverageService.completeSearch(period1Jan.getId(), "testing");

        assertTrue(coverageService.canEOBSearchBeStarted(period1Jan.getId()));
        assertFalse(coverageService.isCoveragePeriodInProgress(period1Jan.getId()));

        assertThrows(
            CoveragePeriodNotFoundException.class,
            () -> {
                coverageService.isCoveragePeriodInProgress(999999999);
            }
        );
    }

    @DisplayName("Count coverage records for a group of coverage periods")
    @Test
    void countForCoveragePeriods() {
        coverageService.submitSearch(period1Jan.getId(), "testing");
        coverageService.submitSearch(period1Feb.getId(), "testing");
        CoverageSearchEvent janProgress = startSearchAndPullEvent();
        CoverageSearchEvent febProgress = startSearchAndPullEvent();

        // Number of beneficiaries shared between months
        // large number to attempt to trigger indexing
        // for more realistic results
        int sharedBeneficiaries = 10000;

        // Add 500 beneficiaries to each month
        Set<Identifiers> identifiers = new LinkedHashSet<>();
        for (long idx = 0; idx < sharedBeneficiaries; idx++) {
            identifiers.add(createIdentifier(idx));
        }

        // Save shared beneficiaries between months
        coverageService.insertCoverage(janProgress.getId(), identifiers);
        coverageService.insertCoverage(febProgress.getId(), identifiers);

        // Add unique beneficiary to January
        coverageService.insertCoverage(janProgress.getId(),
                Set.of(createIdentifier(-1L)));

        // Add unique beneficiaries to February
        coverageService.insertCoverage(febProgress.getId(),
                Set.of(createIdentifier(-2L), createIdentifier(-3L)));

        int januaryCount = coverageService.countBeneficiariesByCoveragePeriod(List.of(period1Jan));
        assertEquals(sharedBeneficiaries +1, januaryCount);

        int februaryCount = coverageService.countBeneficiariesByCoveragePeriod(List.of(period1Feb));
        assertEquals(sharedBeneficiaries + 2, februaryCount);

        // Recognize that there are records not in January that are in February and vice versa
        // but don't count beneficiaries shared by these coverage periods
        int combinedCount = coverageService.countBeneficiariesByCoveragePeriod(List.of(period1Jan, period1Feb));
        assertEquals(sharedBeneficiaries + 3, combinedCount);
    }

    @DisplayName("Count coverage records for a group of contracts")
    @Test
    void countForContracts() {
        coverageService.submitSearch(period1Jan.getId(), "testing");
        coverageService.submitSearch(period1Feb.getId(), "testing");
        coverageService.submitSearch(period1March.getId(), "testing");
        coverageService.submitSearch(period1April.getId(), "testing");

        CoveragePeriod period2Feb = dataSetup.createCoveragePeriod("TST-34", 2, 2020);
        CoveragePeriod period2March = dataSetup.createCoveragePeriod("TST-34", 3, 2020);
        CoveragePeriod period2April = dataSetup.createCoveragePeriod("TST-34", 4, 2020);
        CoveragePeriod period2May = dataSetup.createCoveragePeriod("TST-34", 5, 2020);

        coverageService.submitSearch(period2Jan.getId(), "testing");
        coverageService.submitSearch(period2Feb.getId(), "testing");
        coverageService.submitSearch(period2March.getId(), "testing");
        coverageService.submitSearch(period2April.getId(), "testing");
        coverageService.submitSearch(period2May.getId(), "testing");

        CoverageSearchEvent jan1Progress = startSearchAndPullEvent();
        startSearchAndPullEvent();
        CoverageSearchEvent march1Progress = startSearchAndPullEvent();
        CoverageSearchEvent april1Progress = startSearchAndPullEvent();

        CoverageSearchEvent jan2Progress = startSearchAndPullEvent();
        startSearchAndPullEvent();
        CoverageSearchEvent march2Progress = startSearchAndPullEvent();
        startSearchAndPullEvent();
        CoverageSearchEvent may2Progress = startSearchAndPullEvent();

        // Number of beneficiaries shared between months
        // large number to attempt to trigger indexing
        // for more realistic results
        int sharedBeneficiaries = 10000;

        Set<Identifiers> fullIdentifiers = new LinkedHashSet<>();
        for (long idx = 0; idx < sharedBeneficiaries; idx++) {
            fullIdentifiers.add(createIdentifier(idx));
        }

        Set<Identifiers> halfIdentifiers = fullIdentifiers.stream().limit(5000).collect(toSet());

        // Save shared beneficiaries between months
        coverageService.insertCoverage(jan1Progress.getId(), fullIdentifiers);
        coverageService.insertCoverage(march1Progress.getId(), halfIdentifiers);
        coverageService.insertCoverage(april1Progress.getId(), fullIdentifiers);
        coverageService.completeSearch(period1Jan.getId(), "testing");
        coverageService.completeSearch(period1Feb.getId(), "testing");
        coverageService.completeSearch(period1March.getId(), "testing");
        coverageService.completeSearch(period1April.getId(), "testing");

        coverageService.insertCoverage(jan2Progress.getId(), halfIdentifiers);
        coverageService.insertCoverage(march2Progress.getId(), fullIdentifiers);
        coverageService.insertCoverage(may2Progress.getId(), halfIdentifiers);
        coverageService.completeSearch(period2Jan.getId(), "testing");
        coverageService.completeSearch(period2Feb.getId(), "testing");
        coverageService.completeSearch(period2March.getId(), "testing");
        coverageService.completeSearch(period2April.getId(), "testing");
        coverageService.completeSearch(period2May.getId(), "testing");

        List<CoverageCount> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contract1, contract2));

        assertNotNull(coverageCounts);
        assertEquals(6, coverageCounts.size());
        // No duplicate coverage periods or events
        assertEquals(coverageCounts.size(), coverageCounts.stream().map(CoverageCount::getCoveragePeriodId).collect(toSet()).size());
        assertEquals(coverageCounts.size(), coverageCounts.stream().map(CoverageCount::getCoverageEventId).collect(toSet()).size());

        Map<Integer, CoverageCount> coverageCountMap = coverageCounts.stream()
                .collect(Collectors.toMap(CoverageCount::getCoveragePeriodId, Function.identity()));

        assertTrue(coverageCountMap.containsKey(period1Jan.getId()));
        assertEquals(fullIdentifiers.size(), coverageCountMap.get(period1Jan.getId()).getBeneficiaryCount());

        assertFalse(coverageCountMap.containsKey(period1Feb.getId()));

        assertTrue(coverageCountMap.containsKey(period1March.getId()));
        assertEquals(halfIdentifiers.size(), coverageCountMap.get(period1March.getId()).getBeneficiaryCount());

        assertTrue(coverageCountMap.containsKey(period1April.getId()));
        assertEquals(fullIdentifiers.size(), coverageCountMap.get(period1April.getId()).getBeneficiaryCount());

        assertTrue(coverageCountMap.containsKey(period2Jan.getId()));
        assertEquals(halfIdentifiers.size(), coverageCountMap.get(period2Jan.getId()).getBeneficiaryCount());

        assertFalse(coverageCountMap.containsKey(period2Feb.getId()));

        assertTrue(coverageCountMap.containsKey(period2March.getId()));
        assertEquals(fullIdentifiers.size(), coverageCountMap.get(period2March.getId()).getBeneficiaryCount());

        assertFalse(coverageCountMap.containsKey(period2April.getId()));

        assertTrue(coverageCountMap.containsKey(period2May.getId()));
        assertEquals(halfIdentifiers.size(), coverageCountMap.get(period2May.getId()).getBeneficiaryCount());
    }

    @DisplayName("DB structure matches JPA")
    @Test
    void verifyDBStructure() {

        CoverageSearchEvent cs1 = coverageService.submitSearch(period1Jan.getId(), "testing").get();

        assertNotNull(cs1.getId());

        CoverageSearchEvent csCopy = coverageSearchEventRepo.findById(cs1.getId()).get();

        assertNotNull(csCopy.getCoveragePeriod());
        assertNull(csCopy.getOldStatus());
        assertNotNull(csCopy.getNewStatus());
        assertNotNull(csCopy.getDescription());

        assertEquals(cs1.getCoveragePeriod(), csCopy.getCoveragePeriod());
        assertEquals(cs1.getDescription(), csCopy.getDescription());
        assertEquals(cs1.getNewStatus(), csCopy.getNewStatus());

        // Submitting a coverage search added row to coverage search table

        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        assertTrue(search.isPresent());
        assertNotNull(search.get().getCreated());
        assertEquals(period1Jan, search.get().getPeriod());
    }

    @DisplayName("Check search status on CoveragePeriod")
    @Test
    void findLastEvent() {

        Optional<CoverageSearchEvent> lastEvent = coverageService.findMostRecentEvent(period1Jan.getId());

        assertTrue(lastEvent.isEmpty());

        CoverageSearchEvent submission = coverageService.submitSearch(period1Jan.getId(), "testing").get();
        lastEvent = coverageService.findMostRecentEvent(period1Jan.getId());

        assertTrue(lastEvent.isPresent());
        assertEquals(submission.getId(), lastEvent.get().getId());
        assertNull(lastEvent.get().getOldStatus());
        assertEquals(CoverageJobStatus.SUBMITTED, lastEvent.get().getNewStatus());

        CoverageSearchEvent inProgress = startSearchAndPullEvent();
        lastEvent = coverageService.findMostRecentEvent(period1Jan.getId());

        assertTrue(lastEvent.isPresent());
        assertEquals(inProgress.getId(), lastEvent.get().getId());
        assertEquals(CoverageJobStatus.SUBMITTED, lastEvent.get().getOldStatus());
        assertEquals(CoverageJobStatus.IN_PROGRESS, lastEvent.get().getNewStatus());

    }

    @DisplayName("Insert coverage events into database")
    @Test
    void insertCoverage() {
        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress = startSearchAndPullEvent();

        // Check that beneficiary IDs are correctly preserved

        Set<Identifiers> identifiers = Set.of(createIdentifier(123L), createIdentifier(456L),
                createIdentifier(789L));
        List<Long> originalBeneIds = identifiers.stream().map(Identifiers::getBeneficiaryId)
                                        .collect(toList());

        CoverageSearchEvent savedTo = coverageService.insertCoverage(inProgress.getId(), identifiers);

        assertEquals(inProgress, savedTo);

        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(1000, null, contract1, jobStartTime);
        CoveragePagingResult result = coverageServiceRepo.pageCoverage(pagingRequest);
        List<Long> savedBeneficiaryIds = result.getCoverageSummaries().stream()
                .map(summary -> summary.getIdentifiers().getBeneficiaryId()).collect(toList());

        assertTrue(savedBeneficiaryIds.containsAll(originalBeneIds));
        assertTrue(originalBeneIds.containsAll(savedBeneficiaryIds));

        // Check that MBIs are correctly preserved

        List<Coverage> coverage = dataSetup.findCoverage();

        coverage.forEach(cov -> {
            Long beneId = cov.getBeneficiaryId();
            String mbi = cov.getCurrentMbi();

            Long mbiNumber = Long.parseLong(mbi.substring(mbi.indexOf('-') + 1));

            assertEquals(beneId, mbiNumber);
        });
    }

    @DisplayName("Page coverage from database correctly provides next requests")
    @Test
    void pageCoverage() {
        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress = startSearchAndPullEvent();

        // Last page will have only one id
        int totalBeneficiaries = 501;
        int pageSize = 250;

        // Add 700 beneficiaries to
        Set<Identifiers> identifiers = new LinkedHashSet<>();
        for (long idx = 0; idx < totalBeneficiaries; idx++) {
            identifiers.add(createIdentifier(idx));
        }

        CoverageSearchEvent savedTo = coverageService.insertCoverage(inProgress.getId(), identifiers);

        assertEquals(inProgress, savedTo);

        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(pageSize, null, contract1, jobStartTime);

        // Complete first request which should return exactly 333 results, and the next
        CoveragePagingResult pagingResult = coverageServiceRepo.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();

        assertEquals(pageSize, coverageSummaries.size());
        assertTrue(pagingResult.getNextRequest().isPresent());

        pagingRequest = pagingResult.getNextRequest().get();
        assertTrue(pagingRequest.getCursor().isPresent());

        assertEquals(pageSize, pagingRequest.getPageSize());


        // Complete second request which should return exactly 333
        Long cursor = pagingRequest.getCursor().get();
        pagingResult = coverageServiceRepo.pageCoverage(pagingRequest);
        coverageSummaries = pagingResult.getCoverageSummaries();
        coverageSummaries.sort(Comparator.comparing(summary -> summary.getIdentifiers().getBeneficiaryId()));

        assertEquals(cursor, coverageSummaries.get(0).getIdentifiers().getBeneficiaryId());
        assertEquals(pageSize, coverageSummaries.size());
        assertTrue(pagingResult.getNextRequest().isPresent());

        pagingRequest = pagingResult.getNextRequest().get();
        assertTrue(pagingRequest.getCursor().isPresent());

        assertEquals(pageSize, pagingRequest.getPageSize());

        // Complete third request should return one record with no next cursor
        cursor = pagingRequest.getCursor().get();
        pagingResult = coverageServiceRepo.pageCoverage(pagingRequest);
        coverageSummaries = pagingResult.getCoverageSummaries();
        coverageSummaries.sort(Comparator.comparing(summary -> summary.getIdentifiers().getBeneficiaryId()));

        assertEquals(cursor, coverageSummaries.get(0).getIdentifiers().getBeneficiaryId());
        assertEquals(1, coverageSummaries.size());
        assertTrue(pagingResult.getNextRequest().isEmpty());
    }

    @DisplayName("Page coverage only returns beneficiaries from the right contract only")
    @Test
    void pageCoverageOnlyReturnsBeneficiariesForContract() {

        dataSetup.createCoveragePeriod("TST-34", 2020, 2);
        dataSetup.createCoveragePeriod("TST-34", 2020, 3);
        dataSetup.createCoveragePeriod("TST-34", 2020, 4);

        coverageService.submitSearch(period1Jan.getId(), "testing");
        coverageService.submitSearch(period2Jan.getId(), "testing");
        CoverageSearchEvent inProgressContract1 = startSearchAndPullEvent();
        CoverageSearchEvent inProgressContract2 = startSearchAndPullEvent();

        // Last page will have only one id
        int totalBeneficiaries = 500;
        int pageSize = 1000;

        // Add 700 beneficiaries to
        Set<Identifiers> identifiersContract1 = new LinkedHashSet<>();
        for (long idx = 0; idx < totalBeneficiaries; idx++) {
            identifiersContract1.add(createIdentifier(idx));
        }

        Set<Identifiers> identifiersContract2 = new LinkedHashSet<>();
        for (long idx = 500; idx < 500 + totalBeneficiaries; idx++) {
            identifiersContract1.add(createIdentifier(idx));
        }

        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(inProgressContract1.getId(), identifiersContract1);
        CoverageSearchEvent savedTo2 = coverageService.insertCoverage(inProgressContract2.getId(), identifiersContract2);

        assertEquals(inProgressContract1, savedTo1);
        assertEquals(inProgressContract2, savedTo2);

        CoveragePagingRequest pagingRequest1 = new CoveragePagingRequest(pageSize, null, contract1, jobStartTime);

        CoveragePagingResult pagingResult1 = coverageServiceRepo.pageCoverage(pagingRequest1);
        List<CoverageSummary> coverageSummaries1 = pagingResult1.getCoverageSummaries();

        assertTrue(coverageSummaries1.stream().map(CoverageSummary::getIdentifiers).allMatch(identifiersContract1::contains));
        assertTrue(coverageSummaries1.stream().map(CoverageSummary::getIdentifiers).noneMatch(identifiersContract2::contains));

        CoveragePagingRequest pagingRequest2 = new CoveragePagingRequest(pageSize, null, contract2, jobStartTime);

        CoveragePagingResult pagingResult2 = coverageServiceRepo.pageCoverage(pagingRequest2);
        List<CoverageSummary> coverageSummaries2 = pagingResult2.getCoverageSummaries();

        assertTrue(coverageSummaries2.stream().map(CoverageSummary::getIdentifiers).noneMatch(identifiersContract1::contains));
        assertTrue(coverageSummaries2.stream().map(CoverageSummary::getIdentifiers).allMatch(identifiersContract2::contains));

    }

    @DisplayName("Page coverage when number of beneficiaries enrollment is one more than page size")
    @Test
    void pageCoverageEdgeCase() {
        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress = startSearchAndPullEvent();

        // Last page will have only one id
        int totalBeneficiaries = 500;
        int pageSize = 500;

        // Add 700 beneficiaries to
        Set<Identifiers> identifiers = new LinkedHashSet<>();
        for (long idx = 0; idx < totalBeneficiaries; idx++) {
            identifiers.add(createIdentifier(idx));
        }

        CoverageSearchEvent savedTo = coverageService.insertCoverage(inProgress.getId(), identifiers);

        assertEquals(inProgress, savedTo);

        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(pageSize, null, contract1, jobStartTime);

        // Complete third request should return one record with no next cursor
        CoveragePagingResult pagingResult = coverageServiceRepo.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();
        coverageSummaries.sort(Comparator.comparing(summary -> summary.getIdentifiers().getBeneficiaryId()));

        assertEquals(pageSize, coverageSummaries.size());
        assertTrue(pagingResult.getNextRequest().isEmpty());
    }

    @DisplayName("Coverage summary whole period or nothing")
    @Test
    void coverageSummaryWholePeriod() {

        /*
         * testing-1  is a member for all months (January to April)
         * testing-2 is a member for no periods
         */

        // Bootstrap coverage periods

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgressJan = startSearchAndPullEvent();

        coverageService.submitSearch(period1Feb.getId(), "testing");
        CoverageSearchEvent inProgressFeb = startSearchAndPullEvent();

        coverageService.submitSearch(period1March.getId(), "testing");
        CoverageSearchEvent inProgressMarch = startSearchAndPullEvent();

        coverageService.submitSearch(period1April.getId(), "testing");
        CoverageSearchEvent inProgressApril = startSearchAndPullEvent();


        Identifiers testing1 = createIdentifier(1L);
        coverageService.insertCoverage(inProgressJan.getId(), Set.of(testing1));
        coverageService.insertCoverage(inProgressFeb.getId(), Set.of(testing1));
        coverageService.insertCoverage(inProgressMarch.getId(), Set.of(testing1));
        coverageService.insertCoverage(inProgressApril.getId(), Set.of(testing1));


        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(2, null,
                contract1, jobStartTime);
        CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();

        assertEquals(1, coverageSummaries.size());

        CoverageSummary summary = coverageSummaries.get(0);
        assertEquals(testing1.getBeneficiaryId(), summary.getIdentifiers().getBeneficiaryId());
        assertEquals(testing1.getCurrentMbi(), summary.getIdentifiers().getCurrentMbi());

        assertTrue(summary.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_DEC)));

        assertTrue(summary.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_APRIL)));
        assertFalse(summary.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

    }

    @DisplayName("Coverage summary for most months")
    @Test
    void selectCoverageAllButOneMonth() {

        /*
         * testing-1 is a member for three months (January to March)
         * testing-2 is a member for three months (February to April)
         */

        // Bootstrap coverage periods

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgressJan = startSearchAndPullEvent();

        coverageService.submitSearch(period1Feb.getId(), "testing");
        CoverageSearchEvent inProgressFeb = startSearchAndPullEvent();

        coverageService.submitSearch(period1March.getId(), "testing");
        CoverageSearchEvent inProgressMarch = startSearchAndPullEvent();

        coverageService.submitSearch(period1April.getId(), "testing");
        CoverageSearchEvent inProgressApril = startSearchAndPullEvent();

        Identifiers testing1 = createIdentifier(1L);
        Identifiers testing2 = createIdentifier(2L);
        coverageService.insertCoverage(inProgressJan.getId(), Set.of(testing1));
        coverageService.insertCoverage(inProgressFeb.getId(), Set.of(testing1, testing2));
        coverageService.insertCoverage(inProgressMarch.getId(), Set.of(testing1, testing2));
        coverageService.insertCoverage(inProgressApril.getId(), Set.of(testing2));


        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(2, null, contract1, jobStartTime);
        CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();

        coverageSummaries.sort(Comparator.comparing(summary -> summary.getIdentifiers().getBeneficiaryId()));

        assertEquals(2, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals(1, summary1.getIdentifiers().getBeneficiaryId());

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_DEC)));

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_MARCH)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));

        CoverageSummary summary2 = coverageSummaries.get(1);
        assertEquals(2, summary2.getIdentifiers().getBeneficiaryId());

        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_JAN)));

        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_APRIL)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

    }

    @DisplayName("Coverage summary where individual months are in range")
    @Test
    void selectCoverageDisjointMonths() {

        /*
         * testing-1 is a member for two months (January and March)
         * testing-2 is a member for two months (February and April)
         */

        // Bootstrap coverage periods

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgressJan = startSearchAndPullEvent();

        coverageService.submitSearch(period1Feb.getId(), "testing");
        CoverageSearchEvent inProgressFeb = startSearchAndPullEvent();

        coverageService.submitSearch(period1March.getId(), "testing");
        CoverageSearchEvent inProgressMarch = startSearchAndPullEvent();

        coverageService.submitSearch(period1April.getId(), "testing");
        CoverageSearchEvent inProgressApril = startSearchAndPullEvent();



        coverageService.insertCoverage(inProgressJan.getId(), Set.of(createIdentifier(1L)));
        coverageService.insertCoverage(inProgressFeb.getId(), Set.of(createIdentifier(2L)));
        coverageService.insertCoverage(inProgressMarch.getId(), Set.of(createIdentifier(1L)));
        coverageService.insertCoverage(inProgressApril.getId(), Set.of(createIdentifier(2L)));

        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(2, null, contract1, jobStartTime);
        CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();

        coverageSummaries.sort(Comparator.comparing(summary -> summary.getIdentifiers().getBeneficiaryId()));

        assertEquals(2, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals(1, summary1.getIdentifiers().getBeneficiaryId());

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_DEC)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_FEB)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));

        CoverageSummary summary2 = coverageSummaries.get(1);
        assertEquals(2, summary2.getIdentifiers().getBeneficiaryId());

        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_JAN)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));

        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_MARCH)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

    }

    @DisplayName("Coverage summary for odd groupings of months")
    @Test
    void selectCoverageOddMembership() {

        /*
         * testing-1 is a member for three months (January, February, April)
         * testing-2 is a member for three months (January, March, April)
         * testing-3 is a member for two months (February, March)
         */

        // Bootstrap coverage periods

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgressJan = startSearchAndPullEvent();

        coverageService.submitSearch(period1Feb.getId(), "testing");
        CoverageSearchEvent inProgressFeb = startSearchAndPullEvent();

        coverageService.submitSearch(period1March.getId(), "testing");
        CoverageSearchEvent inProgressMarch = startSearchAndPullEvent();

        coverageService.submitSearch(period1April.getId(), "testing");
        CoverageSearchEvent inProgressApril = startSearchAndPullEvent();


        Identifiers testing1 = createIdentifier(1L);
        Identifiers testing2 = createIdentifier(2L);
        Identifiers testing3 = createIdentifier(3L);

        coverageService.insertCoverage(inProgressJan.getId(), Set.of(testing1, testing2));
        coverageService.insertCoverage(inProgressFeb.getId(), Set.of(testing1, testing3));
        coverageService.insertCoverage(inProgressMarch.getId(), Set.of(testing2, testing3));
        coverageService.insertCoverage(inProgressApril.getId(), Set.of(testing1, testing2));


        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(3, null,
                contract1, jobStartTime);
        CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();

        coverageSummaries.sort(Comparator.comparing(summary -> summary.getIdentifiers().getBeneficiaryId()));

        assertEquals(3, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals(1, summary1.getIdentifiers().getBeneficiaryId());

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

        CoverageSummary summary2 = coverageSummaries.get(1);
        assertEquals(2, summary2.getIdentifiers().getBeneficiaryId());

        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

        CoverageSummary summary3 = coverageSummaries.get(2);
        assertEquals(3, summary3.getIdentifiers().getBeneficiaryId());

        assertFalse(summary3.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertTrue(summary3.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertTrue(summary3.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertFalse(summary3.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));
        assertFalse(summary3.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

    }

    @DisplayName("Coverage summary for one month")
    @Test
    void selectCoverageOneMonth() {

        /*
         * testing-1 is a member for three months (February)
         */

        // Bootstrap coverage periods

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgressJan = startSearchAndPullEvent();

        coverageService.submitSearch(period1Feb.getId(), "testing");
        CoverageSearchEvent inProgressFeb = startSearchAndPullEvent();

        coverageService.submitSearch(period1March.getId(), "testing");
        CoverageSearchEvent inProgressMarch = startSearchAndPullEvent();

        coverageService.submitSearch(period1April.getId(), "testing");
        CoverageSearchEvent inProgressApril = startSearchAndPullEvent();


        coverageService.insertCoverage(inProgressFeb.getId(), Set.of(createIdentifier(1L)));

        CoveragePagingRequest pagingRequest = new CoveragePagingRequest(3, null,
                contract1, jobStartTime);
        CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
        List<CoverageSummary> coverageSummaries = pagingResult.getCoverageSummaries();

        assertEquals(1, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals(1, summary1.getIdentifiers().getBeneficiaryId());

        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));

    }

    @DisplayName("Get differences between first search and empty")
    @Test
    void diffCoverageWhenFirstSearch() {
        Set<Identifiers> results1 = Set.of(createIdentifier(1231L),
                createIdentifier(4561L), createIdentifier(7891L));

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(inProgress1.getId(), results1);

        assertEquals(inProgress1, savedTo1);

        CoverageSearchDiff searchDiff = coverageService.searchDiff(period1Jan.getId());
        List<CoverageDelta> deltas = coverageDeltaTestRepository.findAll();
        assertTrue(deltas.isEmpty());

        assertEquals(3, searchDiff.getCurrentCount());
        assertEquals(0, searchDiff.getPreviousCount());
        assertEquals(0, searchDiff.getUnchanged());
        assertEquals(0, searchDiff.getDeletions());
        assertEquals(3, searchDiff.getAdditions());
    }


    @DisplayName("Get differences between two searches")
    @Test
    void diffCoverageBetweenTwoSearches() {
        Set<Identifiers> results1 = Set.of(createIdentifier(1231L),
                createIdentifier(4561L), createIdentifier(7891L));
        Set<Identifiers> results2 = Set.of(createIdentifier(1231L),
                createIdentifier(4562L), createIdentifier(7892L),
                createIdentifier(8900L));

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(inProgress1.getId(), results1);
        coverageService.completeSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress1, savedTo1);

        coverageService.submitSearch(period1Jan.getId(), "testing");

        // Search must currently be in progress or exception is thrown
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.searchDiff(period1Jan.getId()));

        CoverageSearchEvent inProgress2 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo2 = coverageService.insertCoverage(inProgress2.getId(), results2);

        assertEquals(inProgress2, savedTo2);

        CoverageSearchDiff searchDiff = coverageService.searchDiff(period1Jan.getId());
        List<CoverageDelta> deltas = coverageDeltaTestRepository.findAll();
        assertFalse(deltas.isEmpty());
        Map<String, Long> deltaTypeCount =
                deltas.stream().collect(Collectors.groupingBy(CoverageDelta::getType, Collectors.counting()));

        assertEquals(4, searchDiff.getCurrentCount());
        assertEquals(3, searchDiff.getPreviousCount());
        assertEquals(1, searchDiff.getUnchanged());
        assertEquals(2, searchDiff.getDeletions());
        assertEquals(2, deltaTypeCount.get(COVERAGE_DELETED));
        assertEquals(3, searchDiff.getAdditions());
        assertEquals(3, deltaTypeCount.get(COVERAGE_ADDED));
    }

    @DisplayName("Delete all previous search coverage information on completion of a new search")
    @Test
    void deletePreviousSearchOnCompletion() {
        Set<Identifiers> results1 = Set.of(createIdentifier(1231L),
                createIdentifier(4561L), createIdentifier(7891L));
        Set<Long> beneIds1 = results1.stream()
                .map(Identifiers::getBeneficiaryId)
                .collect(toSet());
        Set<Identifiers> results2 = Set.of(createIdentifier(1232L),
                createIdentifier(4562L), createIdentifier(7892L));
        Set<Long> beneIds2 = results2.stream()
                .map(Identifiers::getBeneficiaryId)
                .collect(toSet());

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(inProgress1.getId(), results1);
        coverageService.completeSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress1, savedTo1);

        // Failing a search and somehow deletion doesn't process
        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress2 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo2 = coverageService.insertCoverage(inProgress2.getId(), results1);

        assertEquals(inProgress2, savedTo2);

        // Fail search outside of normal method
        CoverageSearchEvent failSearch = new CoverageSearchEvent();
        failSearch.setCoveragePeriod(period1Jan);
        failSearch.setDescription("testing");
        failSearch.setOldStatus(CoverageJobStatus.IN_PROGRESS);
        failSearch.setNewStatus(CoverageJobStatus.FAILED);
        coverageSearchEventRepo.saveAndFlush(failSearch);
        period1Jan.setStatus(CoverageJobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(period1Jan);

        // Completing a second search should trigger deletion of old data
        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress3 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo3 = coverageService.insertCoverage(inProgress3.getId(), results2);
        coverageService.completeSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress3, savedTo3);

        List<Long> coverages = dataSetup.findCoverage()
                .stream().map(Coverage::getBeneficiaryId).collect(toList());

        assertTrue(disjoint(beneIds1, coverages));
        assertTrue(coverages.containsAll(beneIds2));

        Set<Long> distinctSearchEvents = dataSetup.findCoverage().stream()
                .map(Coverage::getSearchEventId).collect(toSet());

        assertFalse(distinctSearchEvents.contains(inProgress1.getId()));
        assertFalse(distinctSearchEvents.contains(inProgress2.getId()));
        assertTrue(distinctSearchEvents.contains(inProgress3.getId()));
    }

    @DisplayName("Check search status on CoveragePeriod")
    @Test
    void getSearchStatus() {

        CoverageJobStatus status = coverageService.getSearchStatus(period1Jan.getId());

        assertNull(status);

        coverageService.submitSearch(period1Jan.getId(), "testing");

        status = coverageService.getSearchStatus(period1Jan.getId());

        assertEquals(CoverageJobStatus.SUBMITTED, status);
    }

    @DisplayName("Find all coverage periods that have not been searched since x time")
    @Test
    void findSearchedSince() {

        OffsetDateTime startTest = OffsetDateTime.now();

        // Both January periods have never been searched so neither will be returned but
        // this call doesn't check for null values
        List<CoveragePeriod> coveragePeriods = coverageService.coveragePeriodNotUpdatedSince(1, 2020, startTest.minusDays(1));
        assertEquals(0, coveragePeriods.size());

        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();
        CoverageSearchEvent completedEvent = coverageService.completeSearch(period1Jan.getId(), "testing");

        // Searching using future date should return stale january coverage period
        coveragePeriods = coverageService.coveragePeriodNotUpdatedSince(1, 2020, completedEvent.getCreated().plusSeconds(1));
        assertEquals(1, coveragePeriods.size());

        // Searching using past date should return nothing because january period was searched recently
        coveragePeriods = coverageService.coveragePeriodNotUpdatedSince(1, 2020, startTest.minusDays(1));
        assertEquals(0, coveragePeriods.size());
    }

    @Test
    void testCoveragePeriodNeverSearchedSuccessfully() {
        List<CoveragePeriod> coveragePeriodsOne = coverageService.coveragePeriodNeverSearchedSuccessfully();
        assertEquals(5, coveragePeriodsOne.size());

        coverageService.prioritizeSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();

        List<CoveragePeriod> coveragePeriodsTwo = coverageService.coveragePeriodNeverSearchedSuccessfully();
        assertEquals(4, coveragePeriodsTwo.size());
    }

    @Test
    void testStartSearchNull() {
        Optional<CoverageMapping> coverageMapping = coverageService.startSearch(null, "testing");
        assertTrue(coverageMapping.isEmpty());
    }

    @DisplayName("Find all stuck jobs")
    @Test
    void findStuckJobs() {

        OffsetDateTime startTest = OffsetDateTime.now();

        coverageService.submitSearch(period1Jan.getId(), "testing");
        Optional<CoverageSearch> search1 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search1.get());
        coverageService.startSearch(search1.get(), "testing");

        coverageService.submitSearch(period2Jan.getId(), "testing");
        Optional<CoverageSearch> search2 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search2.get());
        Optional<CoverageMapping> coverageMapping = coverageService.startSearch(search2.get(), "testing");
        coverageService.completeSearch(coverageMapping.get().getPeriodId(), "testing");

        coverageService.submitSearch(period1Feb.getId(), "testing");
        Optional<CoverageSearch> search3 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search3.get());
        coverageService.startSearch(search3.get(), "testing");

        OffsetDateTime midTest = OffsetDateTime.now();

        coverageService.submitSearch(period1March.getId(), "testing");
        Optional<CoverageSearch> search4 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search4.get());
        coverageService.startSearch(search4.get(), "testing");

        coverageService.submitSearch(period1April.getId(), "testing");
        Optional<CoverageSearch> search5 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search5.get());
        coverageService.startSearch(search5.get(), "testing");

        OffsetDateTime afterTest = OffsetDateTime.now();

        List<CoveragePeriod> stuckJobs = coverageService.coveragePeriodStuckJobs(afterTest);
        assertEquals(4, stuckJobs.size());
        assertFalse(stuckJobs.stream().anyMatch(p -> p.getId().equals(period2Jan.getId())));
        assertTrue(stuckJobs.stream().anyMatch(p -> p.getId().equals(period1Jan.getId())));
        assertTrue(stuckJobs.stream().anyMatch(p -> p.getId().equals(period1Feb.getId())));
        assertTrue(stuckJobs.stream().anyMatch(p -> p.getId().equals(period1March.getId())));
        assertTrue(stuckJobs.stream().anyMatch(p -> p.getId().equals(period1April.getId())));

        stuckJobs = coverageService.coveragePeriodStuckJobs(midTest);
        assertEquals(2, stuckJobs.size());
        assertTrue(stuckJobs.stream().anyMatch(p -> p.equals(period1Jan)));
        assertTrue(stuckJobs.stream().anyMatch(p -> p.equals(period1Feb)));

        stuckJobs = coverageService.coveragePeriodStuckJobs(startTest);
        assertEquals(0, stuckJobs.size());
    }

    @DisplayName("Coverage period searches are successfully submitted")
    @Test
    void submitSearches() {
        CoverageSearchEvent cs1 = coverageService.submitSearch(period1Jan.getId(), "testing").get();

        CoverageSearchEvent cs1Copy = coverageSearchEventRepo.findById(cs1.getId()).get();
        CoverageSearch coverageSearch = coverageSearchRepo.findFirstByOrderByCreatedAsc().get();

        assertEquals(CoverageJobStatus.SUBMITTED, cs1Copy.getNewStatus());
        // Make sure that coverage search and search event match
        assertEquals(cs1Copy.getCoveragePeriod(), coverageSearch.getPeriod());

        startSearchAndPullEvent();
    }

    @DisplayName("Coverage period searches are successfully started")
    @Test
    void startSearches() {

        coverageService.submitSearch(period1Jan.getId(), "testing");
        coverageService.submitSearch(period2Jan.getId(), "testing");

        CoverageSearchEvent started = startSearchAndPullEvent();
        CoverageSearchEvent startedCopy = coverageSearchEventRepo.findById(started.getId()).get();

        assertEquals(CoverageJobStatus.SUBMITTED, startedCopy.getOldStatus());
        assertEquals(CoverageJobStatus.IN_PROGRESS, startedCopy.getNewStatus());
    }

    @Test
    void testPrioritizeSearches() {
        coverageService.prioritizeSearch(period1Jan.getId(), "testing");
        coverageService.prioritizeSearch(period2Jan.getId(), "testing");

        CoverageSearchEvent started = startSearchAndPullEvent();
        CoverageSearchEvent startedCopy = coverageSearchEventRepo.findById(started.getId()).get();

        assertEquals(CoverageJobStatus.SUBMITTED, startedCopy.getOldStatus());
        assertEquals(CoverageJobStatus.IN_PROGRESS, startedCopy.getNewStatus());
    }

    @Test
    void testPrioritizeSearchReturnEmpty() {
        Optional<CoverageSearchEvent> coverageSearchEventOne = coverageService.prioritizeSearch(period1Jan.getId(), "testing");
        assertFalse(coverageSearchEventOne.isEmpty());

        startSearchAndPullEvent();

        Optional<CoverageSearchEvent> coverageSearchEventTwo = coverageService.prioritizeSearch(period1Jan.getId(), "testing");
        assertTrue(coverageSearchEventTwo.isEmpty());
    }

    @Test
    void testSubmitSearchReturnEmpty() {
        Optional<CoverageSearchEvent> coverageSearchEventOne = coverageService.submitSearch(period1Jan.getId(), "testing");
        assertFalse(coverageSearchEventOne.isEmpty());

        startSearchAndPullEvent();

        Optional<CoverageSearchEvent> coverageSearchEventTwo = coverageService.submitSearch(period1Jan.getId(), "testing");
        assertTrue(coverageSearchEventTwo.isEmpty());
    }

    @DisplayName("Coverage period searches are successfully resubmitted if necessary")
    @Test
    void resubmitSearch() {
        coverageService.submitSearch(period1Jan.getId(), "testing");

        CoverageSearchEvent started = startSearchAndPullEvent();
        CoverageSearchEvent startedCopy = coverageSearchEventRepo.findById(started.getId()).get();

        assertEquals(CoverageJobStatus.SUBMITTED, startedCopy.getOldStatus());
        assertEquals(CoverageJobStatus.IN_PROGRESS, startedCopy.getNewStatus());

        coverageService.resubmitSearch(started.getCoveragePeriod().getId(), 1, "failed job", "restarting job", false);

        CoveragePeriod period = coveragePeriodRepo.findById(started.getCoveragePeriod().getId()).get();

        List<CoverageSearchEvent> events = coverageSearchEventRepo.findAll();

        CoverageSearchEvent failedEvent = events.stream().filter(event -> event.getCoveragePeriod().getId().equals(period.getId()))
                .filter(event -> CoverageJobStatus.FAILED == event.getNewStatus()).findFirst().get();
        assertEquals(CoverageJobStatus.IN_PROGRESS, failedEvent.getOldStatus());
        assertEquals(CoverageJobStatus.FAILED, failedEvent.getNewStatus());
        assertEquals("failed job", failedEvent.getDescription());

        CoverageSearchEvent submitEvent = coverageSearchEventRepo.findFirstByCoveragePeriodOrderByCreatedDesc(period).get();
        assertEquals(CoverageJobStatus.FAILED, submitEvent.getOldStatus());
        assertEquals(CoverageJobStatus.SUBMITTED, submitEvent.getNewStatus());
        assertEquals("restarting job", submitEvent.getDescription());

        assertEquals(CoverageJobStatus.SUBMITTED, period.getStatus());
    }

    @Test
    void resubmitSearchWithPriority() {
        coverageService.submitSearch(period1Jan.getId(), "testing");

        CoverageSearchEvent started = startSearchAndPullEvent();
        CoverageSearchEvent startedCopy = coverageSearchEventRepo.findById(started.getId()).get();

        assertEquals(CoverageJobStatus.SUBMITTED, startedCopy.getOldStatus());
        assertEquals(CoverageJobStatus.IN_PROGRESS, startedCopy.getNewStatus());

        coverageService.resubmitSearch(started.getCoveragePeriod().getId(), 1, "failed job", "restarting job", true);

        CoveragePeriod period = coveragePeriodRepo.findById(started.getCoveragePeriod().getId()).get();

        CoverageSearchEvent submitEvent = coverageSearchEventRepo.findFirstByCoveragePeriodOrderByCreatedDesc(period).get();
        assertEquals(CoverageJobStatus.FAILED, submitEvent.getOldStatus());
        assertEquals(CoverageJobStatus.SUBMITTED, submitEvent.getNewStatus());
        assertEquals("restarting job", submitEvent.getDescription());

        assertEquals(CoverageJobStatus.SUBMITTED, period.getStatus());
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search.get());
        return coverageService.startSearch(search.get(), "testing").get().getCoverageSearchEvent();
    }

    @DisplayName("Coverage period searches can be marked successful")
    @Test
    void completeSearches() {

        coverageService.submitSearch(period1Jan.getId(), "testing");
        coverageService.submitSearch(period2Jan.getId(), "testing");

        // Cannot start search that has not been submitted
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeSearch(period1Feb.getId(), "testing"));

        startSearchAndPullEvent();

        CoverageSearchEvent completed = coverageService.completeSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent completedCopy = coverageSearchEventRepo.findById(completed.getId()).get();

        assertEquals(CoverageJobStatus.IN_PROGRESS, completedCopy.getOldStatus());
        assertEquals(CoverageJobStatus.SUCCESSFUL, completedCopy.getNewStatus());

        // Check that last successful job is updated
        assertNotNull(completedCopy.getCoveragePeriod().getLastSuccessfulJob());

        // Cannot complete job twice
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeSearch(period1Jan.getId(), "testing"));

        // Add status changes that should invalidate marking a job completed

        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();

        CoverageSearchEvent cancel = new CoverageSearchEvent();
        cancel.setCoveragePeriod(period2Jan);
        cancel.setNewStatus(CoverageJobStatus.CANCELLED);
        cancel.setOldStatus(CoverageJobStatus.SUBMITTED);
        cancel.setDescription("testing");

        CoverageSearchEvent failed = new CoverageSearchEvent();
        failed.setCoveragePeriod(period1Jan);
        failed.setNewStatus(CoverageJobStatus.FAILED);
        failed.setOldStatus(CoverageJobStatus.IN_PROGRESS);
        failed.setDescription("testing");

        coverageSearchEventRepo.save(cancel);
        coverageSearchEventRepo.save(failed);

        period1Jan.setStatus(CoverageJobStatus.CANCELLED);
        period2Jan.setStatus(CoverageJobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(period1Jan);
        coveragePeriodRepo.saveAndFlush(period2Jan);

        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeSearch(period1Jan.getId(), "testing"));
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeSearch(period2Jan.getId(), "testing"));
    }

    @DisplayName("Coverage period searches can be cancelled")
    @Test
    void cancelSearches() {

        coverageService.submitSearch(period1Jan.getId(), "testing");

        // Cannot complete search that has not been started
        CoverageSearchEvent cancelled = coverageService.cancelSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent cancelledCopy = coverageSearchEventRepo.findById(cancelled.getId()).get();

        assertEquals(CoverageJobStatus.SUBMITTED, cancelledCopy.getOldStatus());
        assertEquals(CoverageJobStatus.CANCELLED, cancelledCopy.getNewStatus());

        // Cannot cancel job twice
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.cancelSearch(period1Jan.getId(), "testing"));

        // Add status changes that should invalidate marking a job completed
        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();

        // Cannot cancel in progress job
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.cancelSearch(period1Jan.getId(), "testing"));
    }

    @DisplayName("Coverage period searches can be cancelled")
    @Test
    void failSearches() {

        coverageService.submitSearch(period1Jan.getId(), "testing");
        coverageService.submitSearch(period2Jan.getId(), "testing");

        // Cannot fail search that has not been started
        assertThrows(InvalidJobStateTransition.class,
                () -> coverageService.failSearch(period1Jan.getId(), "testing"));

        startSearchAndPullEvent();

        CoverageSearchEvent failed = coverageService.failSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent failedCopy = coverageSearchEventRepo.findById(failed.getId()).get();

        assertEquals(CoverageJobStatus.IN_PROGRESS, failedCopy.getOldStatus());
        assertEquals(CoverageJobStatus.FAILED, failedCopy.getNewStatus());
    }

    @DisplayName("Coverage period searches can be cancelled")
    @Test
    void failSearchesDeletesData() {
        Set<Identifiers> results1 = Set.of(createIdentifier(1231L),
                createIdentifier(4561L), createIdentifier(7891L));

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        coverageService.insertCoverage(inProgress1.getId(), results1);

        coverageService.failSearch(period1Jan.getId(), "testing");

        // Deleting a coverage searches results deletes the data for that search
        Assertions.assertTrue(dataSetup.findCoverage().isEmpty());
    }

    @DisplayName("Coverage period month and year are checked correctly")
    @Test
    void checkMonthAndYear() {

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1, 0, 2020));

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1, 13, 2020));

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1, 12, 2040));

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1, 12, 2019));
    }

    @Test
    void testGetCoveragePeriods() {
        List<CoveragePeriod> coveragePeriodsOne = coverageService.getCoveragePeriods(JANUARY, YEAR);
        assertFalse(coveragePeriodsOne.isEmpty());

        List<CoveragePeriod> coveragePeriodsTwo = coverageService.getCoveragePeriods(MAY, YEAR);
        assertTrue(coveragePeriodsTwo.isEmpty());
    }

    private Identifiers createIdentifier(Long suffix) {
        return new Identifiers(suffix, "mbi-" + suffix, new LinkedHashSet<>());
    }
}
