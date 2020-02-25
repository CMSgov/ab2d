package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Beneficiary;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.BeneficiaryRepository;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
@SpringBootTest
@Testcontainers
class BeneficiaryServiceIntegrationTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired BeneficiaryService cut;

    @Autowired BeneficiaryRepository beneRepo;
    @Autowired ContractRepository contractRepo;
    @Autowired CoverageRepository coverageRepo;
    @Autowired SponsorRepository sponsorRepo;
    @Autowired UserRepository userRepo;

    private Random random = new Random();
    private Contract contract;
    private Set<Beneficiary> beneficiaries = new HashSet<>();
    private Set<String> patientIds;

    @BeforeEach
    void setUp() {
        userRepo.deleteAll();
        coverageRepo.deleteAll();
        sponsorRepo.deleteAll();
        contractRepo.deleteAll();
        beneRepo.deleteAll();

        var sponsor = createSponsor();
        createUser(sponsor);
        contract = createContract(sponsor);

        beneficiaries.add( createBeneficiary());
        beneficiaries.add( createBeneficiary());
        beneficiaries.add( createBeneficiary());
        beneficiaries.add( createBeneficiary());

        patientIds = beneficiaries.stream()
                .map(b -> b.getPatientId())
                .collect(Collectors.toSet());
    }


    @Test
    void findPatientIdsInDb() {

        // Given
        beneficiaries.forEach(beneficiary -> {
            final Coverage coverage = createCoverage(contract, beneficiary, 1);

            contract.getCoverages().add(coverage);
            contractRepo.save(contract);

            beneficiary.getCoverages().add(coverage);
            beneRepo.save(beneficiary);
        });

        final Set<String> patientIdsInDb = cut.findPatientIdsInDb(contract.getId(), 1);

        //Then
        assertFalse(patientIdsInDb.isEmpty());
        assertThat(patientIdsInDb.size(), is(4));
        for (String patientId: patientIdsInDb) {
            assertTrue(beneficiaries.stream().anyMatch(b -> b.getPatientId().equals(patientId)));
        }

    }

    @Test
    void storeBeneficiaries_WhenTheBenesAlreadyExist() {

        cut.storeBeneficiaries(contract.getId(), patientIds, 1);

        beneficiaries.stream().forEach( beneficiary -> {
            final List<Coverage> coveragesSaved = coverageRepo.findByContractAndBeneficiaryAndPartDMonth(contract, beneficiary, 1);
            assertThat(coveragesSaved.size(), is(1));

            final Coverage coverageSaved = coveragesSaved.get(0);
            assertThat(coverageSaved.getBeneficiary().getPatientId(), is(beneficiary.getPatientId()));
            assertThat(coverageSaved.getContract().getContractNumber(), is(contract.getContractNumber()));
            assertThat(coverageSaved.getContract().getContractName(), is(contract.getContractName()));
            assertThat(coverageSaved.getPartDMonth(), is(1));
        });
    }

    @Test
    void storeBeneficiaries_WhenThereAreAFewNewBenes() {

        patientIds.add(Instant.now().toString() + random.nextInt(100));
        cut.storeBeneficiaries(contract.getId(), patientIds, 1);

        beneficiaries.stream().forEach( beneficiary -> {
            final List<Coverage> coveragesSaved = coverageRepo.findByContractAndBeneficiaryAndPartDMonth(contract, beneficiary, 1);
            assertThat(coveragesSaved.size(), is(1));

            final Coverage coverageSaved = coveragesSaved.get(0);
            assertThat(coverageSaved.getBeneficiary().getPatientId(), is(beneficiary.getPatientId()));
            assertThat(coverageSaved.getContract().getContractNumber(), is(contract.getContractNumber()));
            assertThat(coverageSaved.getContract().getContractName(), is(contract.getContractName()));
            assertThat(coverageSaved.getPartDMonth(), is(1));
        });
    }

    private Beneficiary createBeneficiary() {
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setPatientId("patientId" + Instant.now() + random.nextInt(100));
        return beneRepo.save(beneficiary);
    }

    private Coverage createCoverage(Contract contract, Beneficiary bene, int partDMonth) {
        Coverage coverage = new Coverage();
        coverage.setBeneficiary(bene);
        coverage.setContract(contract);
        coverage.setPartDMonth(partDMonth);
        return coverageRepo.save(coverage);
    }





    private Sponsor createSponsor() {
        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent");
        parent.setLegalName("Parent");
        parent.setHpmsId(350);

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Hogwarts School of Wizardry");
        sponsor.setLegalName("Hogwarts School of Wizardry LLC");
        final Random random = new Random();
        sponsor.setHpmsId(random.nextInt());
        sponsor.setParent(parent);
        parent.getChildren().add(sponsor);
        return sponsorRepo.save(sponsor);
    }

    private User createUser(Sponsor sponsor) {
        User user = new User();
        user.setUsername("Harry_Potter");
        user.setFirstName("Harry");
        user.setLastName("Potter");
        user.setEmail("harry_potter@hogwarts.edu");
        user.setEnabled(TRUE);
        user.setSponsor(sponsor);
        return userRepo.save(user);
    }

    private Contract createContract(Sponsor sponsor) {
        Contract contract = new Contract();
        contract.setContractName("CONTRACT_0000");
        contract.setContractNumber("CONTRACT_0000");
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return contractRepo.save(contract);
    }

//    private Job createJob(User user) {
//        Job job = new Job();
//        job.setJobUuid("S0000");
//        job.setStatus(JobStatus.SUBMITTED);
//        job.setStatusMessage("0%");
//        job.setUser(user);
//        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
//        job.setCreatedAt(OffsetDateTime.now());
//        return jobRepository.save(job);
//    }


}