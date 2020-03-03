package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;
import gov.cms.ab2d.common.model.Beneficiary;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.BeneficiaryRepository;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
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

    @Autowired CacheService cut;
    @Autowired BeneficiaryRepository beneRepo;
    @Autowired CoverageRepository coverageRepo;
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
        final int nowNano = Instant.now().getNano();
        contractNumber = "CONTRACT_" + nowNano + "0000";

        sponsor = createSponsor();
        contract = createContract(sponsor, contractNumber);

        createCoverage(contract, createBeneficiary(), january);
        createCoverage(contract, createBeneficiary(), january);
        createCoverage(contract, createBeneficiary(), january);
        createCoverage(contract, createBeneficiary(), january);
        createCoverage(contract, createBeneficiary(), january);
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

        cut.clearCache(request);

        final List<String> activePatientIds = coverageRepo.findActivePatientIds(contract.getId(), january);
        assertTrue(activePatientIds.isEmpty());
    }

    @Test
    void given_contractNumber_only_should_clear_cache() {
        //given
        createCoverage(contract, createBeneficiary(), february);
        createCoverage(contract, createBeneficiary(), march);
        createCoverage(contract, createBeneficiary(), april);
        createCoverage(contract, createBeneficiary(), may);

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
        patientIds.addAll(coverageRepo.findActivePatientIds(contract.getId(), january));
        patientIds.addAll(coverageRepo.findActivePatientIds(contract.getId(), february));
        patientIds.addAll(coverageRepo.findActivePatientIds(contract.getId(), march));
        patientIds.addAll(coverageRepo.findActivePatientIds(contract.getId(), april));
        patientIds.addAll(coverageRepo.findActivePatientIds(contract.getId(), may));

        return patientIds;
    }

    @Test
    void given_month_only_should_clear_cache() {
        //given
        createContractAndCoverage(january);
        createContractAndCoverage(january);
        createContractAndCoverage(january);

        //when
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setMonth(january);
        cut.clearCache(request);

        //then
        final List<String> activePatientIds = coverageRepo.findActivePatientIds(this.contract.getId(), january);
        assertTrue(activePatientIds.isEmpty());
    }


    @Test
    void when_month_and_contractNumber_is_omitted_clear_all_rows_from_table() {
        //given multiple months for a specific contract
        createCoverage(contract, createBeneficiary(), february);
        createCoverage(contract, createBeneficiary(), march);
        createCoverage(contract, createBeneficiary(), april);
        createCoverage(contract, createBeneficiary(), may);

        //given multiple contracts for a specific month
        createContractAndCoverage(january);
        createContractAndCoverage(january);
        createContractAndCoverage(january);

        assertThat(coverageRepo.findAll().size(), is(24));

        //when
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        cut.clearCache(request);

        //then
        assertTrue(coverageRepo.findAll().isEmpty());
    }

    private void createContractAndCoverage(final int month) {
        final String contractNumber = "CONTRACT_" + Instant.now().getNano();
        final Contract contract = createContract(sponsor, contractNumber);

        createCoverage(contract, createBeneficiary(), month);
        createCoverage(contract, createBeneficiary(), month);
        createCoverage(contract, createBeneficiary(), month);
        createCoverage(contract, createBeneficiary(), month);
        createCoverage(contract, createBeneficiary(), month);
    }


    private Beneficiary createBeneficiary() {
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setPatientId("patientId_" + Instant.now().getNano());
        return beneRepo.save(beneficiary);
    }

    private Coverage createCoverage(Contract contract, Beneficiary bene, int partDMonth) {
        Coverage coverage = new Coverage();
        coverage.setBeneficiary(bene);
        coverage.setContract(contract);
        coverage.setPartDMonth(partDMonth);
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