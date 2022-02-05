package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
class HPMSManualModeTest {

    public static final String TEST_CONTRACT_NUMBER = "X1234";
    @Autowired
    private ContractRepository contractRepository;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Qualifier("for_testing")
    @Autowired
    private AttestationUpdaterServiceImpl aus;

    @Test
    void manualLeftAlone() {
        Contract contract = buildContract(Contract.UpdateMode.MANUAL);
        Contract savedContract = contractRepository.save(contract);

        aus.pollOrganizations();
        Optional<Contract> contractOpt = contractRepository.findContractByContractNumber(TEST_CONTRACT_NUMBER);
        assertTrue(contractOpt.isPresent());
        Contract contractResult = contractOpt.get();
        assertNotNull(contractResult.getAttestedOn());

        contractRepository.delete(savedContract);
    }

    @Test
    void clearedOut() {
        Contract contract = buildContract(Contract.UpdateMode.AUTOMATIC);
        Contract savedContract = contractRepository.save(contract);

        aus.pollOrganizations();
        Optional<Contract> contractOpt = contractRepository.findContractByContractNumber(TEST_CONTRACT_NUMBER);
        assertTrue(contractOpt.isPresent());
        Contract contractResult = contractOpt.get();
        assertNull(contractResult.getAttestedOn());

        contractRepository.delete(savedContract);
    }

    @Test
    void orgChanged() {
        final var existingMock = "S1234";
        final var bogusContractName = "bogus";
        Optional<Contract> contractOpt = contractRepository.findContractByContractNumber(existingMock);
        assertTrue(contractOpt.isPresent());
        Contract contractResult = contractOpt.get();
        contractResult.setContractName(bogusContractName);

        /*
        Contract contract = new Contract();
        contract.setContractNumber(existingMock);
        contract.setContractName("bogus");
        Contract savedContract = contractRepository.save(contract);

         */

        aus.pollOrganizations();
        contractOpt = contractRepository.findContractByContractNumber(existingMock);
        assertTrue(contractOpt.isPresent());
        contractResult = contractOpt.get();
        assertNotNull(contractResult.getHpmsParentOrg());
        assertNotEquals("bogus", contractResult.getContractName());
    }

    @NotNull
    private Contract buildContract(Contract.UpdateMode updateMode) {
        Contract contract = new Contract();
        contract.setContractNumber(TEST_CONTRACT_NUMBER);
        contract.setContractName("Manual Mode Test");
        contract.setAttestedOn(OffsetDateTime.now());
        contract.setUpdateMode(updateMode);
        return contract;
    }
}
