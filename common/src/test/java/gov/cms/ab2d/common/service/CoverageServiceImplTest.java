package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CoverageServiceImplTest {

    private static final int YEAR = 2020;
    private static final int JANUARY = 1;
    private static final int FEBRUARY = 2;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    CoverageRepository coverageRepo;

    @Autowired
    CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    CoverageSearchEventRepository coverageSearchEventRepo;

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

    private CoveragePeriod period2Jan;

    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {
        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract1 = dataSetup.setupContract(sponsor, "TST-123");
        contract2 = dataSetup.setupContract(sponsor, "TST-456");

        period1Jan = dataSetup.createCoveragePeriod(contract1, JANUARY, YEAR);
        period1Feb = dataSetup.createCoveragePeriod(contract1, FEBRUARY, YEAR);

        period2Jan = dataSetup.createCoveragePeriod(contract2, JANUARY, YEAR);
    }

    @AfterEach
    public void cleanUp() {
        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
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

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");

        assertTrue(coverageService.canEOBSearchBeStarted(period1Jan.getId()));
        assertFalse(coverageService.isCoveragePeriodInProgress(period1Jan.getId()));

        coverageService.startCoverageSearch(period1Jan.getId(), "testing");

        assertFalse(coverageService.canEOBSearchBeStarted(period1Jan.getId()));
        assertTrue(coverageService.isCoveragePeriodInProgress(period1Jan.getId()));

        coverageService.completeCoverageSearch(period1Jan.getId(), "testing");

        assertTrue(coverageService.canEOBSearchBeStarted(period1Jan.getId()));
        assertFalse(coverageService.isCoveragePeriodInProgress(period1Jan.getId()));
    }

    @DisplayName("DB structure matches JPA")
    @Test
    void verifyDBStructure() {

        CoverageSearchEvent cs1 = coverageService.submitCoverageSearch(period1Jan.getId(), "testing");

        assertNotNull(cs1.getId());

        CoverageSearchEvent csCopy = coverageSearchEventRepo.findById(cs1.getId()).get();

        assertNotNull(csCopy.getCoveragePeriod());
        assertNull(csCopy.getOldStatus());
        assertNotNull(csCopy.getNewStatus());
        assertNotNull(csCopy.getDescription());

        assertEquals(cs1.getCoveragePeriod(), csCopy.getCoveragePeriod());
        assertEquals(cs1.getDescription(), csCopy.getDescription());
        assertEquals(cs1.getNewStatus(), csCopy.getNewStatus());
    }

    @DisplayName("Check search status on CoveragePeriod")
    @Test
    void findLastEvent() {

        Optional<CoverageSearchEvent> lastEvent = coverageService.findLastEvent(period1Jan.getId());

        assertTrue(lastEvent.isEmpty());

        CoverageSearchEvent submission = coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        lastEvent = coverageService.findLastEvent(period1Jan.getId());

        assertTrue(lastEvent.isPresent());
        assertEquals(submission.getId(), lastEvent.get().getId());
        assertNull(lastEvent.get().getOldStatus());
        assertEquals(JobStatus.SUBMITTED, lastEvent.get().getNewStatus());

        CoverageSearchEvent inProgress = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
        lastEvent = coverageService.findLastEvent(period1Jan.getId());

        assertTrue(lastEvent.isPresent());
        assertEquals(inProgress.getId(), lastEvent.get().getId());
        assertEquals(JobStatus.SUBMITTED, lastEvent.get().getOldStatus());
        assertEquals(JobStatus.IN_PROGRESS, lastEvent.get().getNewStatus());

    }

    @DisplayName("Insert coverage events into database")
    @Test
    void insertCoverage() {
        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress = coverageService.startCoverageSearch(period1Jan.getId(), "testing");

        List<String> beneficiaryIds = List.of("testing-123", "testing-456", "testing-789");

        CoverageSearchEvent savedTo = coverageService.insertCoverage(period1Jan.getId(), inProgress.getId(), beneficiaryIds);

        assertEquals(inProgress, savedTo);

        List<String> savedBeneficiaryIds = coverageRepo.findActiveBeneficiaryIds(Collections.singletonList(period1Jan));

        assertTrue(savedBeneficiaryIds.containsAll(beneficiaryIds));
        assertTrue(beneficiaryIds.containsAll(savedBeneficiaryIds));
    }

    @DisplayName("Get differences between first search and empty")
    @Test
    void diffCoverageWhenFirstSearch() {
        List<String> results1 = List.of("testing-123-1", "testing-456-1", "testing-789-1");

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
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
        List<String> results1 = List.of("testing-123-1", "testing-456-1", "testing-789-1");
        List<String> results2 = List.of("testing-123-1", "testing-456-2", "testing-789-2");

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(period1Jan.getId(), inProgress1.getId(), results1);
        coverageService.completeCoverageSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress1, savedTo1);

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress2 = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
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
        List<String> results1 = List.of("testing-123-1", "testing-456-1", "testing-789-1");
        List<String> results2 = List.of("testing-123-2", "testing-456-2", "testing-789-2");

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress1 = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent savedTo1 = coverageService.insertCoverage(period1Jan.getId(), inProgress1.getId(), results1);
        coverageService.completeCoverageSearch(period1Jan.getId(), "testing");

        assertEquals(inProgress1, savedTo1);

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent inProgress2 = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent savedTo2 = coverageService.insertCoverage(period1Jan.getId(), inProgress2.getId(), results2);

        assertEquals(inProgress2, savedTo2);

        coverageService.deletePreviousSearch(period1Jan.getId());

        List<String> coverages = coverageRepo.findAll().stream().map(Coverage::getBeneficiaryId).collect(toList());

        assertTrue(Collections.disjoint(results1, coverages));
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

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");

        status = coverageService.getSearchStatus(period1Jan.getId());

        assertEquals(JobStatus.SUBMITTED, status);
    }

    @DisplayName("Coverage period searches are successfully submitted")
    @Test
    void submitSearches() {
        CoverageSearchEvent cs1 = coverageService.submitCoverageSearch(period1Jan.getId(), "testing");

        CoverageSearchEvent cs1Copy = coverageSearchEventRepo.findById(cs1.getId()).get();

        assertEquals(JobStatus.SUBMITTED, cs1Copy.getNewStatus());
    }

    @DisplayName("Coverage period searches are successfully started")
    @Test
    void startSearches() {

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        coverageService.submitCoverageSearch(period2Jan.getId(), "testing");

        // Cannot start search that has not been submitted
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.startCoverageSearch(period1Feb.getId(), "testing"));

        CoverageSearchEvent started = coverageService.startCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent startedCopy = coverageSearchEventRepo.findById(started.getId()).get();

        assertEquals(JobStatus.SUBMITTED, startedCopy.getOldStatus());
        assertEquals(JobStatus.IN_PROGRESS, startedCopy.getNewStatus());

        // Add status changes that should invalidate marking a job in progress

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

        coverageSearchEventRepo.saveAndFlush(cancel);
        coverageSearchEventRepo.saveAndFlush(failed);

        period1Jan.setStatus(JobStatus.CANCELLED);
        period2Jan.setStatus(JobStatus.FAILED);
        coveragePeriodRepo.saveAndFlush(period1Jan);
        coveragePeriodRepo.saveAndFlush(period2Jan);

        assertThrows(InvalidJobStateTransition.class, () -> coverageService.startCoverageSearch(period1Jan.getId(), "testing"));
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.startCoverageSearch(period2Jan.getId(), "testing"));
    }

    @DisplayName("Coverage period searches can be marked successful")
    @Test
    void completeSearches() {

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        coverageService.submitCoverageSearch(period2Jan.getId(), "testing");

        // Cannot start search that has not been submitted
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeCoverageSearch(period1Feb.getId(), "testing"));

        coverageService.startCoverageSearch(period1Jan.getId(), "testing");

        CoverageSearchEvent completed = coverageService.completeCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent completedCopy = coverageSearchEventRepo.findById(completed.getId()).get();

        assertEquals(JobStatus.IN_PROGRESS, completedCopy.getOldStatus());
        assertEquals(JobStatus.SUCCESSFUL, completedCopy.getNewStatus());

        // Cannot complete job twice
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeCoverageSearch(period1Jan.getId(), "testing"));

        // Add status changes that should invalidate marking a job completed

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        coverageService.startCoverageSearch(period1Jan.getId(), "testing");

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

        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeCoverageSearch(period1Jan.getId(), "testing"));
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.completeCoverageSearch(period2Jan.getId(), "testing"));
    }

    @DisplayName("Coverage period searches can be cancelled")
    @Test
    void cancelSearches() {

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        coverageService.submitCoverageSearch(period2Jan.getId(), "testing");

        // Cannot complete search that has not been started
        CoverageSearchEvent cancelled = coverageService.cancelCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent cancelledCopy = coverageSearchEventRepo.findById(cancelled.getId()).get();

        assertEquals(JobStatus.SUBMITTED, cancelledCopy.getOldStatus());
        assertEquals(JobStatus.CANCELLED, cancelledCopy.getNewStatus());

        // Cannot cancel job twice
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.cancelCoverageSearch(period1Jan.getId(), "testing"));

        // Add status changes that should invalidate marking a job completed
        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        coverageService.startCoverageSearch(period1Jan.getId(), "testing");

        // Cannot cancel in progress job
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.cancelCoverageSearch(period1Jan.getId(), "testing"));
    }

    @DisplayName("Coverage period searches can be cancelled")
    @Test
    void failSearches() {

        coverageService.submitCoverageSearch(period1Jan.getId(), "testing");
        coverageService.submitCoverageSearch(period2Jan.getId(), "testing");

        // Cannot fail search that has not been started
        assertThrows(InvalidJobStateTransition.class, () -> coverageService.failCoverageSearch(period1Jan.getId(), "testing"));

        coverageService.startCoverageSearch(period1Jan.getId(), "testing");

        CoverageSearchEvent failed = coverageService.failCoverageSearch(period1Jan.getId(), "testing");
        CoverageSearchEvent failedCopy = coverageSearchEventRepo.findById(failed.getId()).get();

        assertEquals(JobStatus.IN_PROGRESS, failedCopy.getOldStatus());
        assertEquals(JobStatus.FAILED, failedCopy.getNewStatus());
    }
}
