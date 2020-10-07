package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.common.util.FilterOutByDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.*;

import static java.util.Collections.disjoint;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CoverageServiceImplTest {

    private static final int YEAR = 2020;
    private static final int JANUARY = 1;
    private static final int FEBRUARY = 2;
    private static final int MARCH = 3;
    private static final int APRIL = 4;

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
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    CoverageRepository coverageRepo;

    @Autowired
    CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    CoverageSearchRepository coverageSearchRepo;

    @Autowired
    SponsorRepository sponsorRepo;

    @Autowired
    ContractRepository contractRepo;

    @Autowired
    CoverageService coverageService;

    @Autowired
    DataSetup dataSetup;

    private Sponsor sponsor;

    private Contract contract1;
    private Contract contract2;

    private CoveragePeriod period1Jan;
    private CoveragePeriod period1Feb;
    private CoveragePeriod period1March;
    private CoveragePeriod period1April;

    private CoveragePeriod period2Jan;

    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {
        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract1 = dataSetup.setupContract(sponsor, "TST-123");
        contract2 = dataSetup.setupContract(sponsor, "TST-456");

        period1Jan = dataSetup.createCoveragePeriod(contract1, JANUARY, YEAR);
        period1Feb = dataSetup.createCoveragePeriod(contract1, FEBRUARY, YEAR);
        period1March = dataSetup.createCoveragePeriod(contract1, MARCH, YEAR);
        period1April = dataSetup.createCoveragePeriod(contract1, APRIL, YEAR);

        period2Jan = dataSetup.createCoveragePeriod(contract2, JANUARY, YEAR);
    }

    @AfterEach
    public void cleanUp() {
        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
        contractRepo.delete(contract1);
        contractRepo.delete(contract2);
        contractRepo.flush();

        if (sponsor != null) {
            sponsorRepo.delete(sponsor);
        }
    }

    @DisplayName("Get a coverage period")
    @Test
    void getCoveragePeriod() {
        CoveragePeriod period = coverageService.getCoveragePeriod(contract1.getId(), JANUARY, YEAR);
        assertEquals(period1Jan, period);

        assertThrows(IllegalArgumentException.class, () -> coverageService.getCoveragePeriod(contract1.getId(), JANUARY, 2000));

        assertThrows(IllegalArgumentException.class, () -> coverageService.getCoveragePeriod(contract1.getId(), JANUARY, 2100));
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

        Optional<CoverageSearchEvent> lastEvent = coverageService.findLastEvent(period1Jan.getId());

        assertTrue(lastEvent.isEmpty());

        CoverageSearchEvent submission = coverageService.submitSearch(period1Jan.getId(), "testing").get();
        lastEvent = coverageService.findLastEvent(period1Jan.getId());

        assertTrue(lastEvent.isPresent());
        assertEquals(submission.getId(), lastEvent.get().getId());
        assertNull(lastEvent.get().getOldStatus());
        assertEquals(JobStatus.SUBMITTED, lastEvent.get().getNewStatus());

        CoverageSearchEvent inProgress = startSearchAndPullEvent();
        lastEvent = coverageService.findLastEvent(period1Jan.getId());

        assertTrue(lastEvent.isPresent());
        assertEquals(inProgress.getId(), lastEvent.get().getId());
        assertEquals(JobStatus.SUBMITTED, lastEvent.get().getOldStatus());
        assertEquals(JobStatus.IN_PROGRESS, lastEvent.get().getNewStatus());

    }

    @DisplayName("Insert coverage events into database")
    @Test
    void insertCoverage() {
        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress = startSearchAndPullEvent();

        Set<String> beneficiaryIds = Set.of("testing-123", "testing-456", "testing-789");

        CoverageSearchEvent savedTo = coverageService.insertCoverage(period1Jan.getId(), inProgress.getId(), beneficiaryIds);

        assertEquals(inProgress, savedTo);

        List<String> savedBeneficiaryIds = coverageService.findActiveBeneficiaryIds(0, 1000,
                singletonList(period1Jan.getId()));

        assertTrue(savedBeneficiaryIds.containsAll(beneficiaryIds));
        assertTrue(beneficiaryIds.containsAll(savedBeneficiaryIds));
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


        coverageService.insertCoverage(period1Jan.getId(), inProgressJan.getId(), Set.of("testing-1"));
        coverageService.insertCoverage(period1Feb.getId(), inProgressFeb.getId(), Set.of("testing-1"));
        coverageService.insertCoverage(period1March.getId(), inProgressMarch.getId(), Set.of("testing-1"));
        coverageService.insertCoverage(period1April.getId(), inProgressApril.getId(), Set.of("testing-1"));


        List<CoverageSummary> coverageSummaries = coverageService.pageCoverage(0, 2,
                period1Jan.getId(), period1Feb.getId(), period1March.getId(), period1April.getId());

        assertEquals(1, coverageSummaries.size());

        CoverageSummary summary = coverageSummaries.get(0);
        assertEquals("testing-1", summary.getBeneficiaryId());

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

        coverageService.insertCoverage(period1Jan.getId(), inProgressJan.getId(), Set.of("testing-1"));
        coverageService.insertCoverage(period1Feb.getId(), inProgressFeb.getId(), Set.of("testing-1", "testing-2"));
        coverageService.insertCoverage(period1March.getId(), inProgressMarch.getId(), Set.of("testing-1", "testing-2"));
        coverageService.insertCoverage(period1April.getId(), inProgressApril.getId(), Set.of("testing-2"));


        List<CoverageSummary> coverageSummaries = coverageService.pageCoverage(0, 2,
                period1Jan.getId(), period1Feb.getId(), period1March.getId(), period1April.getId());

        coverageSummaries.sort(Comparator.comparing(CoverageSummary::getBeneficiaryId));

        assertEquals(2, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals("testing-1", summary1.getBeneficiaryId());

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_DEC)));

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_MARCH)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));

        CoverageSummary summary2 = coverageSummaries.get(1);
        assertEquals("testing-2", summary2.getBeneficiaryId());

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


        coverageService.insertCoverage(period1Jan.getId(), inProgressJan.getId(), Set.of("testing-1"));
        coverageService.insertCoverage(period1Feb.getId(), inProgressFeb.getId(), Set.of("testing-2"));
        coverageService.insertCoverage(period1March.getId(), inProgressMarch.getId(), Set.of("testing-1"));
        coverageService.insertCoverage(period1April.getId(), inProgressApril.getId(), Set.of("testing-2"));


        List<CoverageSummary> coverageSummaries = coverageService.pageCoverage(0, 2,
                period1Jan.getId(), period1Feb.getId(), period1March.getId(), period1April.getId());

        coverageSummaries.sort(Comparator.comparing(CoverageSummary::getBeneficiaryId));

        assertEquals(2, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals("testing-1", summary1.getBeneficiaryId());

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_DEC)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(END_FEB)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));

        CoverageSummary summary2 = coverageSummaries.get(1);
        assertEquals("testing-2", summary2.getBeneficiaryId());

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


        coverageService.insertCoverage(period1Jan.getId(), inProgressJan.getId(), Set.of("testing-1", "testing-2"));
        coverageService.insertCoverage(period1Feb.getId(), inProgressFeb.getId(), Set.of("testing-1", "testing-3"));
        coverageService.insertCoverage(period1March.getId(), inProgressMarch.getId(), Set.of("testing-2", "testing-3"));
        coverageService.insertCoverage(period1April.getId(), inProgressApril.getId(), Set.of("testing-1", "testing-2"));


        List<CoverageSummary> coverageSummaries = coverageService.pageCoverage(0, 3,
                period1Jan.getId(), period1Feb.getId(), period1March.getId(), period1April.getId());

        coverageSummaries.sort(Comparator.comparing(CoverageSummary::getBeneficiaryId));

        assertEquals(3, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals("testing-1", summary1.getBeneficiaryId());

        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

        CoverageSummary summary2 = coverageSummaries.get(1);
        assertEquals("testing-2", summary2.getBeneficiaryId());

        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));
        assertTrue(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_APRIL)));
        assertFalse(summary2.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MAY)));

        CoverageSummary summary3 = coverageSummaries.get(2);
        assertEquals("testing-3", summary3.getBeneficiaryId());

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


        coverageService.insertCoverage(period1Feb.getId(), inProgressFeb.getId(), Set.of("testing-1"));

        List<CoverageSummary> coverageSummaries = coverageService.pageCoverage(0, 3,
                period1Jan.getId(), period1Feb.getId(), period1March.getId(), period1April.getId());

        assertEquals(1, coverageSummaries.size());

        CoverageSummary summary1 = coverageSummaries.get(0);
        assertEquals("testing-1", summary1.getBeneficiaryId());

        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_JAN)));
        assertTrue(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_FEB)));
        assertFalse(summary1.getDateRanges().stream().anyMatch(dr -> dr.inRange(START_MARCH)));

    }

    @DisplayName("Get differences between first search and empty")
    @Test
    void diffCoverageWhenFirstSearch() {
        Set<String> results1 = Set.of("testing-123-1", "testing-456-1", "testing-789-1");

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(period1Jan.getId(), inProgress1.getId(), results1);

        assertEquals(inProgress1, savedTo1);

        CoverageSearchDiff searchDiff = coverageService.searchDiff(period1Jan.getId());

        assertEquals(3, searchDiff.getCurrentCount());
        assertEquals(0, searchDiff.getPreviousCount());
        assertEquals(0, searchDiff.getUnchanged());
        assertEquals(0, searchDiff.getDeletions());
        assertEquals(3, searchDiff.getAdditions());
    }


    @DisplayName("Get differences between two searches")
    @Test
    void diffCoverageBetweenTwoSearches() {
        Set<String> results1 = Set.of("testing-123-1", "testing-456-1", "testing-789-1");
        Set<String> results2 = Set.of("testing-123-1", "testing-456-2", "testing-789-2");

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(period1Jan.getId(), inProgress1.getId(), results1);
        coverageService.completeSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress1, savedTo1);

        coverageService.submitSearch(period1Jan.getId(), "testing");

        // Search must currently be in progress or exception is thrown
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.searchDiff(period1Jan.getId()));

        CoverageSearchEvent inProgress2 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo2 = coverageService.insertCoverage(period1Jan.getId(), inProgress2.getId(), results2);

        assertEquals(inProgress2, savedTo2);

        CoverageSearchDiff searchDiff = coverageService.searchDiff(period1Jan.getId());

        assertEquals(3, searchDiff.getCurrentCount());
        assertEquals(3, searchDiff.getPreviousCount());
        assertEquals(1, searchDiff.getUnchanged());
        assertEquals(2, searchDiff.getDeletions());
        assertEquals(2, searchDiff.getAdditions());
    }

    @DisplayName("Delete previous search")
    @Test
    void deletePreviousSearch() {
        Set<String> results1 = Set.of("testing-123-1", "testing-456-1", "testing-789-1");
        Set<String> results2 = Set.of("testing-123-2", "testing-456-2", "testing-789-2");

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(period1Jan.getId(), inProgress1.getId(), results1);
        coverageService.completeSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress1, savedTo1);

        coverageService.submitSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress2 = startSearchAndPullEvent();
        CoverageSearchEvent savedTo2 = coverageService.insertCoverage(period1Jan.getId(), inProgress2.getId(), results2);

        assertEquals(inProgress2, savedTo2);

        coverageService.deletePreviousSearch(period1Jan.getId());

        List<String> coverages = coverageRepo.findAll().stream().map(Coverage::getBeneficiaryId).collect(toList());

        assertTrue(disjoint(results1, coverages));
        assertTrue(coverages.containsAll(results2));

        Set<CoverageSearchEvent> distinctSearchEvents = coverageRepo.findAll().stream()
                .map(Coverage::getCoverageSearchEvent).collect(toSet());

        assertFalse(distinctSearchEvents.contains(inProgress1));
        assertTrue(distinctSearchEvents.contains(inProgress2));
    }

    @DisplayName("Check search status on CoveragePeriod")
    @Test
    void getSearchStatus() {

        JobStatus status = coverageService.getSearchStatus(period1Jan.getId());

        assertNull(status);

        coverageService.submitSearch(period1Jan.getId(), "testing");

        status = coverageService.getSearchStatus(period1Jan.getId());

        assertEquals(JobStatus.SUBMITTED, status);
    }

    @DisplayName("Find all coverage periods that have never had mapping completed")
    @Test
    void getNeverSearched() {
        List<CoveragePeriod> coveragePeriods = coverageService.coveragePeriodNeverSearched();

        assertEquals(5, coveragePeriods.size());

        coverageService.submitSearch(period1Jan.getId(), "testing");

        coveragePeriods = coverageService.coveragePeriodNeverSearched();

        assertEquals(4, coveragePeriods.size());
    }

    @DisplayName("Find all coverage periods that have not been searched since x time")
    @Test
    void findSearchedSince() {

        OffsetDateTime startTest = OffsetDateTime.now();

        // Both January periods have never been searched so both should be returned
        List<CoveragePeriod> coveragePeriods = coverageService.coveragePeriodNotUpdatedSince(1, 2020, startTest.minusDays(1));
        assertEquals(2, coveragePeriods.size());

        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();
        CoverageSearchEvent completedEvent = coverageService.completeSearch(period1Jan.getId(), "testing");

        // Searching using future date should return all (2) January periods
        coveragePeriods = coverageService.coveragePeriodNotUpdatedSince(1, 2020, completedEvent.getCreated().plusSeconds(1));
        assertEquals(2, coveragePeriods.size());

        // Searching using past date should return never searched period only
        coveragePeriods = coverageService.coveragePeriodNotUpdatedSince(1, 2020, startTest.minusDays(1));
        assertEquals(1, coveragePeriods.size());
    }

    @DisplayName("Find all stuck jobs")
    @Test
    void findStuckJobs() {

        OffsetDateTime startTest = OffsetDateTime.now();

        coverageService.submitSearch(period1Jan.getId(), "testing");
        Optional<CoverageSearch> search1 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageService.startSearch(search1.get(), "testing");

        coverageService.submitSearch(period2Jan.getId(), "testing");
        Optional<CoverageSearch> search2 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageService.startSearch(search2.get(), "testing");
        coverageService.completeSearch(period2Jan.getId(), "testing");

        coverageService.submitSearch(period1Feb.getId(), "testing");
        Optional<CoverageSearch> search3 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageService.startSearch(search3.get(), "testing");

        OffsetDateTime midTest = OffsetDateTime.now();

        coverageService.submitSearch(period1March.getId(), "testing");
        Optional<CoverageSearch> search4 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageService.startSearch(search4.get(), "testing");

        coverageService.submitSearch(period1April.getId(), "testing");
        Optional<CoverageSearch> search5 = coverageSearchRepo.findFirstByOrderByCreatedAsc();
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

        assertEquals(JobStatus.SUBMITTED, cs1Copy.getNewStatus());
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

        assertEquals(JobStatus.SUBMITTED, startedCopy.getOldStatus());
        assertEquals(JobStatus.IN_PROGRESS, startedCopy.getNewStatus());
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
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

        assertEquals(JobStatus.IN_PROGRESS, completedCopy.getOldStatus());
        assertEquals(JobStatus.SUCCESSFUL, completedCopy.getNewStatus());

        // Cannot complete job twice
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeSearch(period1Jan.getId(), "testing"));

        // Add status changes that should invalidate marking a job completed

        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();

        CoverageSearchEvent cancel = new CoverageSearchEvent();
        cancel.setCoveragePeriod(period2Jan);
        cancel.setNewStatus(JobStatus.CANCELLED);
        cancel.setOldStatus(JobStatus.SUBMITTED);
        cancel.setDescription("testing");

        CoverageSearchEvent failed = new CoverageSearchEvent();
        failed.setCoveragePeriod(period1Jan);
        failed.setNewStatus(JobStatus.FAILED);
        failed.setOldStatus(JobStatus.IN_PROGRESS);
        failed.setDescription("testing");

        coverageSearchEventRepo.save(cancel);
        coverageSearchEventRepo.save(failed);

        period1Jan.setStatus(JobStatus.CANCELLED);
        period2Jan.setStatus(JobStatus.FAILED);
        coveragePeriodRepo.save(period1Jan);
        coveragePeriodRepo.save(period2Jan);

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

        assertEquals(JobStatus.SUBMITTED, cancelledCopy.getOldStatus());
        assertEquals(JobStatus.CANCELLED, cancelledCopy.getNewStatus());

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

        assertEquals(JobStatus.IN_PROGRESS, failedCopy.getOldStatus());
        assertEquals(JobStatus.FAILED, failedCopy.getNewStatus());
    }

    @DisplayName("Coverage period month and year are checked correctly")
    @Test
    void checkMonthAndYear() {

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1.getId(), 0, 2020));

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1.getId(), 13, 2020));

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1.getId(), 12, 2020));

        assertThrows(IllegalArgumentException.class,
                () -> coverageService.getCoveragePeriod(contract1.getId(), 12, 2019));
    }
}
