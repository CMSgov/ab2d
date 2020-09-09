package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.time.Month.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CacheServiceImplTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();
    public static final int YEAR = 2020;

    @Autowired CacheService cut;
    @Autowired CoverageRepository coverageRepo;
    @Autowired CoverageSearchRepository coverageSearchRepo;
    @Autowired CoverageSearchEventRepository coverageSearchEventRepo;
    @Autowired ContractRepository contractRepo;
    @Autowired SponsorRepository sponsorRepo;

    private final int january = JANUARY.getValue();
    private final int february = FEBRUARY.getValue();;
    private final int march = MARCH.getValue();;
    private final int april = APRIL.getValue();
    private final int may = MAY.getValue();
    private final Random random = new Random();
    private String contractNumber;
    private Contract contract;
    private Sponsor sponsor;


    @BeforeEach
    void setUp() {
        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepo.deleteAll();

        final int nowNano = Instant.now().getNano();
        contractNumber = "CONTRACT_" + nowNano + "0000";

        sponsor = createSponsor();
        contract = createContract(sponsor, contractNumber);

        CoverageSearch coverageSearch = createCoverageSearch(contract, january, YEAR);
        CoverageSearchEvent coverageSearchEvent = createCoverageSearchEvent(coverageSearch, "testing");

        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
    }

    @Test
    void when_month_is_less_than_1_should_throw_exception() {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setMonth(0);

        var exceptionThrown = assertThrows(InvalidUserInputException.class,
                () -> cut.clearCache(request));

        assertThat(exceptionThrown.getMessage(), is("invalid value for month. Month must be between 1 and 12"));
    }

    @Test
    void when_month_is_greater_than_12_should_throw_exception() {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setMonth(13);

        var exceptionThrown = assertThrows(InvalidUserInputException.class,
                () -> cut.clearCache(request));

        assertThat(exceptionThrown.getMessage(), is("invalid value for month. Month must be between 1 and 12"));
    }

    @Test
    void when_contract_number_is_invalid_should_throw_exception() {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setContractNumber("INVALID_CONTRACT_NUMBER");

        var exceptionThrown = assertThrows(InvalidUserInputException.class,
                () -> cut.clearCache(request));

        assertThat(exceptionThrown.getMessage(), is("Contract not found"));
    }

    @Test
    void given_contractNumber_and_month_should_clear_cache() {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setContractNumber(contractNumber);
        request.setMonth(january);
        request.setYear(YEAR);

        cut.clearCache(request);

        CoverageSearch coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), january, YEAR);
        final List<String> activePatientIds = coverageRepo.findActiveBeneficiaryIds(coverageSearch);
        assertTrue(activePatientIds.isEmpty());
    }

    @Test
    void given_contractNumber_only_should_clear_cache() {
        //given
        //given multiple months for a specific contract
        CoverageSearch febCoverage = createCoverageSearch(contract, february, YEAR);
        CoverageSearchEvent febEvent = createCoverageSearchEvent(febCoverage, "testing");

        CoverageSearch marchCoverage = createCoverageSearch(contract, march, YEAR);
        CoverageSearchEvent marchEvent = createCoverageSearchEvent(marchCoverage, "testing");

        CoverageSearch aprilCoverage = createCoverageSearch(contract, april, YEAR);
        CoverageSearchEvent aprilEvent = createCoverageSearchEvent(aprilCoverage, "testing");

        CoverageSearch mayCoverage = createCoverageSearch(contract, may, YEAR);
        CoverageSearchEvent mayEvent = createCoverageSearchEvent(mayCoverage, "testing");

        createCoverage(febCoverage, febEvent, createBeneId());
        createCoverage(marchCoverage, marchEvent, createBeneId());
        createCoverage(aprilCoverage, aprilEvent, createBeneId());
        createCoverage(mayCoverage, mayEvent, createBeneId());

        assertThat(getAllActivePatientIds().size(), is(9));

        //when
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setContractNumber(contractNumber);
        cut.clearCache(request);

        //then
        assertTrue(getAllActivePatientIds().isEmpty());
    }

    private List<String> getAllActivePatientIds() {
        final List<String> patientIds = new ArrayList<>();

        CoverageSearch coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), january, YEAR);
        patientIds.addAll(coverageRepo.findActiveBeneficiaryIds(coverageSearch));

        coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), february, YEAR);
        patientIds.addAll(coverageRepo.findActiveBeneficiaryIds(coverageSearch));

        coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), march, YEAR);
        patientIds.addAll(coverageRepo.findActiveBeneficiaryIds(coverageSearch));

        coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), april, YEAR);
        patientIds.addAll(coverageRepo.findActiveBeneficiaryIds(coverageSearch));

        coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), may, YEAR);
        patientIds.addAll(coverageRepo.findActiveBeneficiaryIds(coverageSearch));

        return patientIds;
    }

    @Test
    void given_month_only_should_clear_cache() {
        //given
        createContractAndCoverage(january, YEAR);
        createContractAndCoverage(january, YEAR);
        createContractAndCoverage(january, YEAR);

        //when
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setMonth(january);
        request.setYear(YEAR);
        cut.clearCache(request);

        //then
        CoverageSearch coverageSearch = coverageSearchRepo.getByContractIdAndMonthAndYear(contract.getId(), january, YEAR);
        final List<String> activePatientIds = coverageRepo.findActiveBeneficiaryIds(coverageSearch);
        assertTrue(activePatientIds.isEmpty());
    }


    @Test
    void when_month_and_contractNumber_is_omitted_clear_all_rows_from_table() {
        //given multiple months for a specific contract
        CoverageSearch febCoverage = createCoverageSearch(contract, february, YEAR);
        CoverageSearchEvent febEvent = createCoverageSearchEvent(febCoverage, "testing");

        CoverageSearch marchCoverage = createCoverageSearch(contract, march, YEAR);
        CoverageSearchEvent marchEvent = createCoverageSearchEvent(marchCoverage, "testing");

        CoverageSearch aprilCoverage = createCoverageSearch(contract, april, YEAR);
        CoverageSearchEvent aprilEvent = createCoverageSearchEvent(aprilCoverage, "testing");

        CoverageSearch mayCoverage = createCoverageSearch(contract, may, YEAR);
        CoverageSearchEvent mayEvent = createCoverageSearchEvent(mayCoverage, "testing");

        createCoverage(febCoverage, febEvent, createBeneId());
        createCoverage(marchCoverage, marchEvent, createBeneId());
        createCoverage(aprilCoverage, aprilEvent, createBeneId());
        createCoverage(mayCoverage, mayEvent, createBeneId());

        //given multiple contracts for a specific month
        createContractAndCoverage(january, YEAR);
        createContractAndCoverage(january, YEAR);
        createContractAndCoverage(january, YEAR);

        assertThat(coverageRepo.findAll().size(), is(24));
    }

    private void createContractAndCoverage(final int month, final int year) {
        final String contractNumber = "CONTRACT_" + Instant.now().getNano();
        final Contract contract = createContract(sponsor, contractNumber);
        final CoverageSearch coverageSearch = createCoverageSearch(contract, month, year);
        final CoverageSearchEvent coverageSearchEvent = createCoverageSearchEvent(coverageSearch, "testing");

        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
        createCoverage(coverageSearch, coverageSearchEvent, createBeneId());
    }

    private String createBeneId() {
        return "patientId_" + Instant.now().getNano();
    }

    private CoverageSearch createCoverageSearch(Contract contract, int month, int year) {
        CoverageSearch coverageSearch = new CoverageSearch();
        coverageSearch.setContract(contract);
        coverageSearch.setMonth(month);
        coverageSearch.setYear(year);

        return coverageSearchRepo.save(coverageSearch);
    }

    private CoverageSearchEvent createCoverageSearchEvent(CoverageSearch coverageSearch, String description) {
        CoverageSearchEvent coverageSearchEvent = new CoverageSearchEvent();
        coverageSearchEvent.setCoverageSearch(coverageSearch);
        coverageSearchEvent.setNewStatus(JobStatus.SUBMITTED);
        coverageSearchEvent.setOccuredAt(OffsetDateTime.now(ZoneOffset.UTC));
        coverageSearchEvent.setDescription(description);

        return coverageSearchEventRepo.save(coverageSearchEvent);
    }

    private Coverage createCoverage(CoverageSearch coverageSearch, CoverageSearchEvent coverageSearchEvent, String bene) {
        Coverage coverage = new Coverage();
        coverage.setCoverageSearch(coverageSearch);
        coverage.setCoverageSearchEvent(coverageSearchEvent);
        coverage.setBeneficiaryId(bene);
        return coverageRepo.save(coverage);
    }

    private Contract createContract(Sponsor sponsor, final String contractNumber) {
        Contract contract = new Contract();
        contract.setContractName(contractNumber);
        contract.setContractNumber(contractNumber);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return contractRepo.save(contract);
    }

    private Sponsor createSponsor() {
        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent");
        parent.setLegalName("Parent");
        parent.setHpmsId(350);

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Hogwarts School of Wizardry");
        sponsor.setLegalName("Hogwarts School of Wizardry LLC");

        sponsor.setHpmsId(random.nextInt());
        sponsor.setParent(parent);
        parent.getChildren().add(sponsor);
        return sponsorRepo.save(sponsor);
    }


}