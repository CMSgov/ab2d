package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.profiles.active=prod")
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class ContractServiceImplTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @MockBean
    private ContractFeignClient contractRepo;

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
        when(contractRepo.getContracts(anyLong())).thenReturn(List.of(contract.toDTO()));
        when(contractRepo.getContractByNumber(anyString())).thenReturn(contract.toDTO());

        dataSetup.queueForCleanup(contract);
    }

    @AfterEach
    private void cleanup() {
        dataSetup.cleanup();
    }

    @DisplayName("Find all contracts and filter by attestation")
    @Test
    void contractServiceFilterAttestation() {

        List<Contract> allContracts = List.of(contract);
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

    @Test
    void testUpdateContract() {
        assertDoesNotThrow(() -> {
            contractService.updateContract(contract);
        });
    }

}
