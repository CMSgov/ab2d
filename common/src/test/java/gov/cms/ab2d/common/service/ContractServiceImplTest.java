package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class ContractServiceImplTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private ContractServiceImpl contractService;

    @Autowired
    private DataSetup dataSetup;

    private Contract contract;

    @BeforeEach
    private void before() {
        contract = new Contract();
        contract.setContractName("Test Contract");
        contract.setContractNumber("NATTE");

        contract = contractRepo.save(contract);    }

    @AfterEach
    private void after() {
        if (contract != null) {
            dataSetup.deleteContract(contract);
        }
    }

    @DisplayName("Find all contracts and filter by attestation")
    @Test
    void contractServiceFilterAttestation() {

        List<Contract> allContracts = contractRepo.findAll();
        List<Contract> attestedContracts = contractService.getAllAttestedContracts();

        // All manually added contracts are automatically attested so these lists should match
        assertEquals(allContracts.size() - 1, attestedContracts.size());
        assertTrue(allContracts.containsAll(attestedContracts));
        assertFalse(attestedContracts.contains(contract));

        attestedContracts.forEach(contract -> assertNotNull(contract.getESTAttestationTime()));
    }

    @DisplayName("Get a contract with an appropriate number")
    @Test
    void contractGetNumber() {
        Optional<Contract> retrieved = contractService.getContractByContractNumber("NATTE");

        assertTrue(retrieved.isPresent());

        assertEquals("Test Contract", retrieved.get().getContractName());
        assertEquals("NATTE", retrieved.get().getContractNumber());

        assertEquals(contract, retrieved.get());
    }
}
