package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Beneficiary;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.repository.BeneficiaryRepository;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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

//    @BeforeEach
//    void setUp() {
//    }

//    @AfterEach
//    void tearDown() {
//    }

    @Test
    void findPatientIdsInDb() {
    }

    @Test
    @Transactional
    void storeBeneficiaries() {

//        final Optional<Contract> s0000 = contracts.stream().filter(c -> c.getContractNumber().equalsIgnoreCase("S0000")).findAny();
//        final Contract contractS0000 = s0000.get();

//        contracts.forEach(c -> {
//            log.info("Contract:: id:[{}]-number:[{}]-name:[{}]-attestedOn:[{}]",
//                    c.getId(), c.getContractNumber(), c.getContractName(), c.getAttestedOn());
//            Contract contract = c;
//            Beneficiary beneficiary = new Beneficiary();
//            beneficiary.setPatientId("patientId");
//            contract.addBeneficiary(beneficiary);
//            contractRepo.save(contract);
//        });

        final Contract contractS0000 = new Contract();
        contractS0000.setContractNumber("S0000");
        contractS0000.setContractName("S0000");
        contractS0000.setAttestedOn(OffsetDateTime.now().minusDays(2));
            log.info("Contract:: id:[{}] - number:[{}] - name:[{}] - attestedOn:[{}]",
                    contractS0000.getId(),
                    contractS0000.getContractNumber(),
                    contractS0000.getContractName(),
                    contractS0000.getAttestedOn());

            Contract contract = contractS0000;
        final Beneficiary beneficiary = createBeneficiary();
        if (beneficiary == null) {
            log.info("Beneficiary is NULL");
        }

        final Coverage coverage = createCoverage(contract, beneficiary, 1);
        //TODO: Need to come up with an alternative for this.
//        contract.addBeneficiary(beneficiary);
        final Contract savedContract = contractRepo.save(contract);


//        final Optional<Contract> optContract = contractRepo.findContractByContractNumber(contractS0000.getContractNumber());
//        final Contract contract2 = optContract.get();
//        contract2.getCoverages().stream().map(cov -> {
//        contract2.getCoverages().forEach(cov -> {

        final Set<Coverage> coverages = savedContract.getCoverages();
        if (coverages == null) {
            log.info("Coverages is NULL ... ");
        } else if (coverages.isEmpty()) {
            log.info("Coverages is empty() ... ");
        } else {
            final int coverageSize = coverages.size();
            log.info("There are [{}] coverages for contract [{}]", coverages.size(), savedContract);
            coverages.forEach(cov -> {
                final Beneficiary beneficiary1 = cov.getBeneficiary();
                if (beneficiary1 == null) {
                    log.info("beneficiary1 is NULL");
                } else {
                    final Long id = beneficiary1.getId();
                    if (id == null) {
                        log.info("id is NULL");
                    }
                    final String patientId = beneficiary1.getPatientId();
                    if (patientId == null) {
                        log.info("patientId is NULL");
                    }
                    log.info(" Beneficiary :: id:[{}] -  patientId:[{}]", id, patientId);
                }
            });
        }
//        cut.storeBeneficiaries();

    }

    private Beneficiary createBeneficiary() {
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setPatientId("patientId");
        return beneRepo.save(beneficiary);
    }

    private Coverage createCoverage(Contract contract, Beneficiary bene, int partDMonth) {
        Coverage coverage = new Coverage();
        coverage.setBeneficiary(bene);
        coverage.setContract(contract);
        coverage.setPartDMonth(partDMonth);
        return coverageRepo.save(coverage);
    }
}