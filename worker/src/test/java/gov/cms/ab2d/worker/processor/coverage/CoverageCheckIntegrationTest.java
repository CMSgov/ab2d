package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
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
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(properties = "coverage.update.initial.delay=1000000")
@Testcontainers
public class CoverageCheckIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @Autowired
    private ContractServiceStub contractServiceStub;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private CoverageDriver coverageDriver;

    @Autowired
    private PdpClientService pdpClientService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private CoverageDataSetup coverageDataSetup;

    @Autowired
    private ContractService contractService;

    private static final ZonedDateTime CURRENT_TIME = OffsetDateTime.now().atZoneSameInstant(AB2D_ZONE);
    private static final ZonedDateTime ATTESTATION_TIME = CURRENT_TIME.minusMonths(3);

    private Contract contract;
    private List<Contract> enabledContracts;
    private CoveragePeriod attestationMonth;
    private CoveragePeriod attestationMonthPlus1;
    private CoveragePeriod attestationMonthPlus2;
    private CoveragePeriod attestationMonthPlus3;

    @BeforeEach
    void setUp() {
        enabledContracts = pdpClientService.getAllEnabledContracts();
        enabledContracts.forEach(contract -> pdpClientService.disableClient(contract.getContractNumber()));

        PdpClient client = dataSetup.setupNonStandardClient("special", "TEST", List.of("SPONSOR"));
        contract = contractService.getContractByContractNumber("TEST").get();
        contract.setAttestedOn(ATTESTATION_TIME.toOffsetDateTime());
        contractServiceStub.updateContract(contract);

    }

    @AfterEach
    void tearDown() {
        enabledContracts.forEach(contract -> pdpClientService.enableClient(contract.getContractNumber()));
        coverageDataSetup.cleanup();
        dataSetup.cleanup();
    }


    @DisplayName("Verify coverage does not have false positives")
    @Test
    void verifyCoverage_whenExpectedCoveragePass() {
        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, tenK);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        try {
            coverageDriver.verifyCoverage();
        } catch (CoverageVerificationException exception) {
            fail("Failed coverage checks with alerts: " + exception.getAlertMessage());
        }
    }

    @DisplayName("Verify coverage ignores contracts being updated")
    @Test
    void verifyCoverage_whenUpdateInProgressPass() {
        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        coverageService.submitSearch(attestationMonth.getId(), "testing");

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () -> coverageDriver.verifyCoverage());

        assertTrue(exception.getAlertMessage().contains("being updated now"));

        startSearchAndPullEvent();

        exception = assertThrows(CoverageVerificationException.class, () -> coverageDriver.verifyCoverage());

        assertTrue(exception.getAlertMessage().contains("being updated now"));
    }

    @DisplayName("Verify coverage ignores contracts being updated")
    @Test
    void verifyCoverage_whenZContractIgnore() {

        PdpClient client = dataSetup.setupNonStandardClient("special2", "Z5555", List.of("SPONSOR"));
        contract = contractService.getContractByContractNumber("Z5555").get();
        contract.setAttestedOn(ATTESTATION_TIME.toOffsetDateTime());
        contract.setUpdateMode(Contract.UpdateMode.NONE);
        contract.setContractType(Contract.ContractType.CLASSIC_TEST);
        contractServiceStub.updateContract(contract);

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () -> coverageDriver.verifyCoverage());

        assertFalse(exception.getAlertMessage().contains("Z5555"));
    }

    @DisplayName("Verify coverage stops if coverage periods are missing entirely")
    @Test
    void verifyCoverage_whenMissingPeriods_fail() {
        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () ->coverageDriver.verifyCoverage());

        assertTrue(exception.getAlertMessage().contains("coverage period missing"));
        assertFalse(exception.getAlertMessage().contains("has no enrollment"));
        assertFalse(exception.getAlertMessage().contains("no enrollment found"));
        assertFalse(exception.getAlertMessage().contains("enrollment changed"));
        assertFalse(exception.getAlertMessage().contains("sets of enrollment"));
        assertFalse(exception.getAlertMessage().contains("old coverage search"));
    }

    @DisplayName("Verify coverage stops if none of the coverage periods have enrollment")
    @Test
    void verifyCoverage_whenNoCoveragePresent_fail() {
        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        runSearch(attestationMonth);
        runSearch(attestationMonthPlus1);
        runSearch(attestationMonthPlus2);

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () ->coverageDriver.verifyCoverage());
        assertFalse(exception.getAlertMessage().contains("coverage period missing"));
        assertTrue(exception.getAlertMessage().contains("has no enrollment"));
        assertFalse(exception.getAlertMessage().contains("no enrollment found"));
        assertFalse(exception.getAlertMessage().contains("enrollment changed"));
        assertFalse(exception.getAlertMessage().contains("sets of enrollment"));
        assertFalse(exception.getAlertMessage().contains("old coverage search"));
    }

    @DisplayName("Verify coverage stops if some coverage periods are missing coverage information")
    @Test
    void verifyCoverage_whenSomeCoverageMissing_fail() {
        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        runSearch(attestationMonthPlus1);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () -> coverageDriver.verifyCoverage());
        assertFalse(exception.getAlertMessage().contains("coverage period missing"));
        assertFalse(exception.getAlertMessage().contains("has no enrollment"));
        assertTrue(exception.getAlertMessage().contains("no enrollment found"));
        assertFalse(exception.getAlertMessage().contains("enrollment changed"));
        assertFalse(exception.getAlertMessage().contains("sets of enrollment"));
        assertFalse(exception.getAlertMessage().contains("old coverage search"));
    }

    @DisplayName("Verify coverage stops if some coverage periods change drastically")
  //  @Test
    void verifyCoverage_whenCoverageUnstable_fail() {
        ZonedDateTime dateTime = ZonedDateTime.now().withMonth(9).withDayOfMonth(1);
        createCoveragePeriods(dateTime);

        contract.setAttestedOn(ZonedDateTime.now().withMonth(9).withDayOfMonth(1).toOffsetDateTime());
        contractServiceStub.updateContract(contract);


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

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () ->coverageDriver.verifyCoverage());
        assertFalse(exception.getAlertMessage().contains("coverage period missing"));
        assertFalse(exception.getAlertMessage().contains("has no enrollment"));
        assertFalse(exception.getAlertMessage().contains("no enrollment found"));
        assertTrue(exception.getAlertMessage().contains("enrollment changed"));
        assertFalse(exception.getAlertMessage().contains("sets of enrollment"));
        assertFalse(exception.getAlertMessage().contains("old coverage search"));
    }

    @DisplayName("Verify coverage stops if some coverage periods have multiple copies of coverage")
    @Test
    void verifyCoverage_whenCoverageDuplicated_fail() {
        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, tenK);
        insertAndLeaveDuplicates(attestationMonthPlus1, tenK);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () ->coverageDriver.verifyCoverage());

        assertFalse(exception.getAlertMessage().contains("coverage period missing"));
        assertFalse(exception.getAlertMessage().contains("has no enrollment"));
        assertFalse(exception.getAlertMessage().contains("no enrollment found"));
        assertFalse(exception.getAlertMessage().contains("enrollment changed"));
        assertTrue(exception.getAlertMessage().contains("sets of enrollment"));
        assertFalse(exception.getAlertMessage().contains("old coverage search"));
    }

    @DisplayName("Verify coverage stops if some coverage periods have old coverage when they should have more recent coverage")
    @Test
    void verifyCoverage_whenCoverageOutOfDate_fail() {
        createCoveragePeriods();

        Set<Identifiers> tenK = new LinkedHashSet<>();
        for (long idx = 0; idx < 10000; idx++) {
            tenK.add(createIdentifier(idx));
        }

        insertAndRunSearch(attestationMonth, tenK);
        insertAndRunSearch(attestationMonthPlus1, tenK);
        runSearchAndLeaveOld(attestationMonthPlus1);
        insertAndRunSearch(attestationMonthPlus2, tenK);

        CoverageVerificationException exception =
                assertThrows(CoverageVerificationException.class, () ->coverageDriver.verifyCoverage());
        assertFalse(exception.getAlertMessage().contains("coverage period missing"));
        assertFalse(exception.getAlertMessage().contains("has no enrollment"));
        assertFalse(exception.getAlertMessage().contains("no enrollment found"));
        assertFalse(exception.getAlertMessage().contains("enrollment changed"));
        assertFalse(exception.getAlertMessage().contains("sets of enrollment"));
        assertTrue(exception.getAlertMessage().contains("old coverage search"));
    }

    private void createCoveragePeriods() {
        createCoveragePeriods(ATTESTATION_TIME);
    }

    private void createCoveragePeriods( ZonedDateTime dateTime) {
        attestationMonth = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), dateTime.getMonthValue(),  dateTime.getYear());
        attestationMonthPlus1 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), dateTime.plusMonths(1).getMonthValue(),
                dateTime.plusMonths(1).getYear());
        attestationMonthPlus2 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), dateTime.plusMonths(2).getMonthValue(),
                dateTime.plusMonths(2).getYear());
        attestationMonthPlus3 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), dateTime.plusMonths(3).getMonthValue(),
                dateTime.plusMonths(3).getYear());
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
        success.setNewStatus(CoverageJobStatus.SUCCESSFUL);
        success.setOldStatus(CoverageJobStatus.IN_PROGRESS);
        coverageSearchEventRepo.saveAndFlush(success);

        period = coveragePeriodRepo.findById(period.getId()).get();
        period.setStatus(CoverageJobStatus.SUCCESSFUL);
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
        success.setNewStatus(CoverageJobStatus.SUCCESSFUL);
        success.setOldStatus(CoverageJobStatus.IN_PROGRESS);
        coverageSearchEventRepo.saveAndFlush(success);

        period = coveragePeriodRepo.findById(period.getId()).get();
        period.setStatus(CoverageJobStatus.SUCCESSFUL);
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