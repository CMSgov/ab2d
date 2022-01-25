package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.model.JobStatus;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "coverage.update.initial.delay=1000000")
@Testcontainers
public class CoverageCheckPredicatesIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private CoverageDataSetup coverageDataSetup;

    @Autowired
    private DataSetup dataSetup;

    private static final ZonedDateTime CURRENT_TIME = OffsetDateTime.now().atZoneSameInstant(AB2D_ZONE);
    private static final ZonedDateTime ATTESTATION_TIME = CURRENT_TIME.minusMonths(3);

    private Contract contract;
    private ContractForCoverageDTO contractForCoverageDTO;
    private CoveragePeriod attestationMonth;
    private CoveragePeriod attestationMonthPlus1;
    private CoveragePeriod attestationMonthPlus2;
    private CoveragePeriod attestationMonthPlus3;

    @BeforeEach
    void setUp() {

        contract = dataSetup.setupContract("TEST", ATTESTATION_TIME.toOffsetDateTime());
        contractForCoverageDTO = new ContractForCoverageDTO("TEST", ATTESTATION_TIME.toOffsetDateTime());
    }

    @AfterEach
    void tearDown() {
        coverageDataSetup.cleanup();
        dataSetup.cleanup();
    }

    @DisplayName("Coverage periods outright missing for contract is detected")
    @Test
    void whenCoveragePeriodsMissing_failPresentCheck() {

        List<String> issues = new ArrayList<>();
        CoveragePeriodsPresentCheck presentCheck = new CoveragePeriodsPresentCheck(coverageService, null, issues);

        presentCheck.test(contract);

        assertEquals(4, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("coverage period missing")));

    }

    @DisplayName("Coverage periods all present for contract check passes")
    @Test
    void whenCoveragePeriodsPresent_passPresentCheck() {
        createCoveragePeriods();

        List<String> issues = new ArrayList<>();
        CoveragePeriodsPresentCheck presentCheck = new CoveragePeriodsPresentCheck(coverageService, null, issues);

        presentCheck.test(contract);

        assertTrue(issues.isEmpty());
    }

    @DisplayName("No coverage periods with data then fails check")
    @Test
    void whenCoveragePeriodsNoCoveragePresentNothing_failCoveragePresentCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoveragePresentCheck presentCheck =
                new CoveragePresentCheck(coverageService, coverageCounts, issues);

        // Fail when no coverage is present
        assertFalse(presentCheck.test(contract));
        assertEquals(1, issues.size());
        assertEquals("TEST has no enrollment", issues.get(0));
    }

    @DisplayName("Single coverage period missing data then fails check")
    @Test
    void whenCoveragePeriodsNoCoveragePresentFinal_failCoveragePresentCheck() {
        createCoveragePeriods();

        insertAndRunSearch(attestationMonth, Set.of(createIdentifier(1L)));
        insertAndRunSearch(attestationMonthPlus1, Set.of(createIdentifier(1L)));

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoveragePresentCheck presentCheck =
                new CoveragePresentCheck(coverageService, coverageCounts, issues);

        assertFalse(presentCheck.test(contract));
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("no enrollment found"));
    }

    @DisplayName("Single coverage period missing data then fails check")
    @Test
    void whenCoveragePeriodsNoCoveragePresentIntermediate_failCoveragePresentCheck() {
        createCoveragePeriods();

        insertAndRunSearch(attestationMonth, Set.of(createIdentifier(1L)));
        insertAndRunSearch(attestationMonthPlus2, Set.of(createIdentifier(1L)));

        // Fail if intermediate period missing
        // But also check remaining periods and do not alert for them
        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoveragePresentCheck presentCheck =
                new CoveragePresentCheck(coverageService, coverageCounts, issues);

        assertFalse(presentCheck.test(contract));

        assertEquals(1, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("no enrollment found")));
    }

    @DisplayName("Coverage periods have no coverage present")
    @Test
    void whenCoveragePeriodsPresent_passCoveragePresentCheck() {

        createCoveragePeriods();

        insertAndRunSearch(attestationMonth, Set.of(createIdentifier(1L)));
        insertAndRunSearch(attestationMonthPlus1, Set.of(createIdentifier(1L)));
        insertAndRunSearch(attestationMonthPlus2, Set.of(createIdentifier(1L)));
        insertAndRunSearch(attestationMonthPlus3, Set.of(createIdentifier(1L)));

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoveragePresentCheck presentCheck =
                new CoveragePresentCheck(coverageService, coverageCounts, issues);

        assertTrue(presentCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Coverage changes are limited to 10% between months fails when changes are large")
    @Test
    void whenCoverageUnstable_failCoverageStabilityCheck() {

        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        Set<Identifiers> twelveK = new LinkedHashSet<>();
        for (long idx = 0; idx < 12000; idx++) {
            twelveK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, twelveK);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        coverageCounts.get(contract.getContractNumber())
                .forEach(count -> assertTrue(count.getBeneficiaryCount() >= 10000));

        List<String> issues = new ArrayList<>();
        CoverageStableCheck stableCheck =
                new CoverageStableCheck(coverageService, coverageCounts, issues);

        assertFalse(stableCheck.test(contract));

        assertEquals(2, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("enrollment changed")));
        assertTrue(issues.get(0).contains("20%"));
    }

    @DisplayName("Coverage changes are limited to 10% between months passes when changes are 1000 benes or less")
    @Test
    void whenCoverageChangesAreSmall_passCoverageStableCheck() {

        createCoveragePeriods();

        Set<Identifiers> oneHundred = new LinkedHashSet<>();
        for (long idx = 0; idx < 100; idx++) {
            oneHundred.add(createIdentifier(idx));
        }

        Set<Identifiers> twoHundred = new LinkedHashSet<>();
        for (long idx = 0; idx < 200; idx++) {
            twoHundred.add(createIdentifier(idx));
        }

        Set<Identifiers> twelveHundred = new LinkedHashSet<>();
        for (long idx = 0; idx < 1199; idx++) {
            twelveHundred.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, oneHundred);
        insertAndRunSearch(attestationMonthPlus1, twoHundred);
        insertAndRunSearch(attestationMonthPlus2, twelveHundred);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        coverageCounts.get(contract.getContractNumber())
                .forEach(count -> assertTrue(count.getBeneficiaryCount() >= 100));

        List<String> issues = new ArrayList<>();
        CoverageStableCheck stableCheck =
                new CoverageStableCheck(coverageService, coverageCounts, issues);

        assertTrue(stableCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Coverage changes are limited to 10% between months passes when true")
    @Test
    void whenCoverageSmallPercentage_passCoverageStableCheck() {

        createCoveragePeriods();

        Set<Identifiers> hundredK = new LinkedHashSet<>();
        for (long idx = 0; idx < 100_000; idx++) {
            hundredK.add(createIdentifier(idx));
        }

        Set<Identifiers> hundredTenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 110_000; idx++) {
            hundredTenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, hundredK);
        insertAndRunSearch(attestationMonthPlus1, hundredTenK);
        insertAndRunSearch(attestationMonthPlus2, hundredK);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        coverageCounts.get(contract.getContractNumber())
                .forEach(count -> assertTrue(count.getBeneficiaryCount() >= 100_000));

        List<String> issues = new ArrayList<>();
        CoverageStableCheck stableCheck =
                new CoverageStableCheck(coverageService, coverageCounts, issues);

        assertTrue(stableCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Duplicate enrollment counts cause a failure")
    @Test
    void whenCoverageDuplicated_failCoverageDuplicationCheck() {

        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, tenK);
        insertAndLeaveDuplicates(attestationMonthPlus1, tenK);
        insertAndRunSearch(attestationMonthPlus2, tenK);
        insertAndLeaveDuplicates(attestationMonthPlus2, tenK);
        insertAndLeaveDuplicates(attestationMonthPlus2, tenK);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoverageNoDuplicatesCheck duplicatesCheck =
                new CoverageNoDuplicatesCheck(coverageService, coverageCounts, issues);

        assertFalse(duplicatesCheck.test(contract));

        assertEquals(2, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("sets of enrollment")));
    }

    @DisplayName("No duplicate enrollment found")
    @Test
    void whenCoverageNotDuplicated_passCoverageDuplicatedCheck() {

        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, tenK);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoverageNoDuplicatesCheck duplicatesCheck =
                new CoverageNoDuplicatesCheck(coverageService, coverageCounts, issues);

        assertTrue(duplicatesCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Out of date enrollment causes a failure")
    @Test
    void whenCoverageOutOfDate_failCoverageDateCheck() {

        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        runSearchAndLeaveOld(attestationMonth);

        insertAndRunSearch(attestationMonthPlus1, tenK);
        runSearchAndLeaveOld(attestationMonthPlus1);

        insertAndRunSearch(attestationMonthPlus2, tenK);
        runSearchAndLeaveOld(attestationMonthPlus2);

        insertAndRunSearch(attestationMonthPlus3, tenK);
        runSearchAndLeaveOld(attestationMonthPlus3);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoverageUpToDateCheck upToDateCheck = new CoverageUpToDateCheck(coverageService, coverageCounts, issues);

        assertFalse(upToDateCheck.test(contract));

        assertEquals(4, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("old coverage search")));
    }

    @DisplayName("Up to date enrollment passes")
    @Test
    void whenCoverageUpToDate_passCoverageUpToDateCheck() {

        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, tenK);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(List.of(contractForCoverageDTO))
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        List<String> issues = new ArrayList<>();
        CoverageUpToDateCheck upToDateCheck =
                new CoverageUpToDateCheck(coverageService, coverageCounts, issues);

        assertTrue(upToDateCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    private void createCoveragePeriods() {
        attestationMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), ATTESTATION_TIME.getMonthValue(),  ATTESTATION_TIME.getYear());
        attestationMonthPlus1 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), ATTESTATION_TIME.plusMonths(1).getMonthValue(),
                ATTESTATION_TIME.plusMonths(1).getYear());
        attestationMonthPlus2 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), ATTESTATION_TIME.plusMonths(2).getMonthValue(),
                ATTESTATION_TIME.plusMonths(2).getYear());
        attestationMonthPlus3 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), ATTESTATION_TIME.plusMonths(3).getMonthValue(),
                ATTESTATION_TIME.plusMonths(3).getYear());
    }

    private void insertAndRunSearch(CoveragePeriod period, Set<Identifiers> identifiers) {
        coverageService.submitSearch(period.getId(), "testing");
        CoverageSearchEvent progress = startSearchAndPullEvent();
        coverageService.insertCoverage(progress.getId(), identifiers);
        coverageService.completeSearch(period.getId(), "testing");
    }

    private void insertAndLeaveDuplicates(CoveragePeriod period, Set<Identifiers> identifiers) {
        coverageService.submitSearch(period.getId(), "testing");
        CoverageSearchEvent progress = startSearchAndPullEvent();
        coverageService.insertCoverage(progress.getId(), identifiers);

        CoverageSearchEvent success = new CoverageSearchEvent();
        success.setCoveragePeriod(period);
        success.setDescription("testing");
        success.setNewStatus(JobStatus.SUCCESSFUL);
        success.setOldStatus(JobStatus.IN_PROGRESS);
        coverageSearchEventRepo.saveAndFlush(success);

        period = coveragePeriodRepo.findById(period.getId()).get();
        period.setStatus(JobStatus.SUCCESSFUL);
        coveragePeriodRepo.saveAndFlush(period);
    }

    private void runSearch(CoveragePeriod period) {
        coverageService.submitSearch(period.getId(), "testing");
        startSearchAndPullEvent();
        coverageService.completeSearch(period.getId(), "testing");
    }

    private void runSearchAndLeaveOld(CoveragePeriod period) {
        coverageService.submitSearch(period.getId(), "testing");
        startSearchAndPullEvent();

        CoverageSearchEvent success = new CoverageSearchEvent();
        success.setCoveragePeriod(period);
        success.setDescription("testing");
        success.setNewStatus(JobStatus.SUCCESSFUL);
        success.setOldStatus(JobStatus.IN_PROGRESS);
        coverageSearchEventRepo.saveAndFlush(success);

        period = coveragePeriodRepo.findById(period.getId()).get();
        period.setStatus(JobStatus.SUCCESSFUL);
        coveragePeriodRepo.saveAndFlush(period);
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search.get());
        return coverageService.startSearch(search.get(), "testing").get().getCoverageSearchEvent();
    }

    private Identifiers createIdentifier(Long suffix) {
        return new Identifiers(suffix, "mbi-" + suffix, new LinkedHashSet<>());
    }
}
