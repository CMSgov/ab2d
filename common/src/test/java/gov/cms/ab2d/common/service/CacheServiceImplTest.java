package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.time.Month.*;
import static java.util.Collections.singletonList;
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
    @Autowired CoveragePeriodRepository coveragePeriodRepo;
    @Autowired CoverageSearchEventRepository coverageSearchEventRepo;
    @Autowired CoverageServiceRepository coverageServiceRepo;
    @Autowired ContractRepository contractRepo;
    @Autowired SponsorRepository sponsorRepo;
    @Autowired CoverageService coverageService;
    @Autowired DataSetup dataSetup;

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

        final int nowNano = Instant.now().getNano();
        contractNumber = "CONTRACT_" + nowNano + "0000";

        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract = dataSetup.setupContract(sponsor, contractNumber);

        CoveragePeriod coveragePeriod = dataSetup.createCoveragePeriod(contract, january, YEAR);
        CoverageSearchEvent coverageSearchEvent = dataSetup.createCoverageSearchEvent(coveragePeriod, "testing");

        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
    }

    @AfterEach
    void teardDown() {
        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
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

        CoveragePeriod coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), january, YEAR);
        final List<String> activePatientIds = coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId()));
        assertTrue(activePatientIds.isEmpty());
    }

    @Test
    void given_contractNumber_only_should_clear_cache() {
        //given
        //given multiple months for a specific contract
        CoveragePeriod febCoverage = dataSetup.createCoveragePeriod(contract, february, YEAR);
        CoverageSearchEvent febEvent = dataSetup.createCoverageSearchEvent(febCoverage, "testing");

        CoveragePeriod marchCoverage = dataSetup.createCoveragePeriod(contract, march, YEAR);
        CoverageSearchEvent marchEvent = dataSetup.createCoverageSearchEvent(marchCoverage, "testing");

        CoveragePeriod aprilCoverage = dataSetup.createCoveragePeriod(contract, april, YEAR);
        CoverageSearchEvent aprilEvent = dataSetup.createCoverageSearchEvent(aprilCoverage, "testing");

        CoveragePeriod mayCoverage = dataSetup.createCoveragePeriod(contract, may, YEAR);
        CoverageSearchEvent mayEvent = dataSetup.createCoverageSearchEvent(mayCoverage, "testing");

        dataSetup.createCoverage(febCoverage, febEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(marchCoverage, marchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(aprilCoverage, aprilEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(mayCoverage, mayEvent, DataSetup.createBeneId());

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

        CoveragePeriod coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), january, YEAR);
        patientIds.addAll(coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId())));

        coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), february, YEAR);
        patientIds.addAll(coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId())));

        coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), march, YEAR);
        patientIds.addAll(coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId())));

        coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), april, YEAR);
        patientIds.addAll(coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId())));

        coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), may, YEAR);
        patientIds.addAll(coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId())));

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
        CoveragePeriod coveragePeriod = coveragePeriodRepo.getByContractIdAndMonthAndYear(contract.getId(), january, YEAR);
        final List<String> activePatientIds = coverageServiceRepo.findActiveBeneficiaryIds(0, 1000,
                singletonList(coveragePeriod.getId()));
        assertTrue(activePatientIds.isEmpty());
    }


    @Test
    void when_month_and_contractNumber_is_omitted_clear_all_rows_from_table() {
        //given multiple months for a specific contract
        CoveragePeriod febCoverage = dataSetup.createCoveragePeriod(contract, february, YEAR);
        CoverageSearchEvent febEvent = dataSetup.createCoverageSearchEvent(febCoverage, "testing");

        CoveragePeriod marchCoverage = dataSetup.createCoveragePeriod(contract, march, YEAR);
        CoverageSearchEvent marchEvent = dataSetup.createCoverageSearchEvent(marchCoverage, "testing");

        CoveragePeriod aprilCoverage = dataSetup.createCoveragePeriod(contract, april, YEAR);
        CoverageSearchEvent aprilEvent = dataSetup.createCoverageSearchEvent(aprilCoverage, "testing");

        CoveragePeriod mayCoverage = dataSetup.createCoveragePeriod(contract, may, YEAR);
        CoverageSearchEvent mayEvent = dataSetup.createCoverageSearchEvent(mayCoverage, "testing");

        dataSetup.createCoverage(febCoverage, febEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(marchCoverage, marchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(aprilCoverage, aprilEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(mayCoverage, mayEvent, DataSetup.createBeneId());

        //given multiple contracts for a specific month
        createContractAndCoverage(january, YEAR);
        createContractAndCoverage(january, YEAR);
        createContractAndCoverage(january, YEAR);

        assertThat(coverageRepo.findAll().size(), is(24));
    }

    private void createContractAndCoverage(final int month, final int year) {
        final String contractNumber = "CONTRACT_" + Instant.now().getNano();
        final Contract contract = dataSetup.setupContract(sponsor, contractNumber);
        final CoveragePeriod coveragePeriod = dataSetup.createCoveragePeriod(contract, month, year);
        final CoverageSearchEvent coverageSearchEvent = dataSetup.createCoverageSearchEvent(coveragePeriod, "testing");

        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
        dataSetup.createCoverage(coveragePeriod, coverageSearchEvent, DataSetup.createBeneId());
    }
}