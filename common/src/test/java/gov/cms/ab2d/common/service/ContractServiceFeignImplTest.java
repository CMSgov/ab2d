package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = { "contract.service.enabled=true" })
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class ContractServiceFeignImplTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @MockBean
    private ContractFeignClient contractFeignClient;

    @Autowired
    private ContractServiceImpl contractService;

    @DisplayName("Find all contracts and filter by attestation")
    @Test
    void contractServiceFilterAttestation() {
        ContractDTO contractDTO = new ContractDTO(42L, "NATTE", "Test Contract", OffsetDateTime.now(), Contract.ContractType.NORMAL);
        List<ContractDTO> allContracts = new ArrayList<>();
        allContracts.add(contractDTO);
        contractDTO = new ContractDTO(43L, "not real", "Test Contract",null, Contract.ContractType.NORMAL);
        allContracts.add(contractDTO);
        when(contractFeignClient.getContracts(any())).thenReturn(allContracts);
        List<Contract> attestedContracts = contractService.getAllAttestedContracts();

        assertEquals(1, attestedContracts.size());
        assertEquals(allContracts.get(0).getContractNumber(), attestedContracts.get(0).getContractNumber());
        assertEquals(allContracts.get(0).getContractName(), attestedContracts.get(0).getContractName());
        assertEquals(allContracts.get(0).getContractType(), attestedContracts.get(0).getContractType());
        assertEquals(allContracts.get(0).getId(), attestedContracts.get(0).getId());
    }

    @DisplayName("Get a contract with an appropriate number")
    @Test
    void contractGetNumber() {
        ContractDTO contractDTO = new ContractDTO(42L, "NATTE", "Test Contract", OffsetDateTime.now(), Contract.ContractType.NORMAL);
        when(contractFeignClient.getContractByNumber(any())).thenReturn(List.of(contractDTO));

        Optional<Contract> retrieved = contractService.getContractByContractNumber("NATTE");

        assertTrue(retrieved.isPresent());

        assertEquals("Test Contract", retrieved.get().getContractName());
        assertEquals("NATTE", retrieved.get().getContractNumber());
    }

    @DisplayName("Get a contract with an appropriate id")
    @Test
    void contractGetID() {
        ContractDTO contractDTO = new ContractDTO(42L, "NATTE", "Test Contract", OffsetDateTime.now(), Contract.ContractType.NORMAL);
        when(contractFeignClient.getContracts(any())).thenReturn(List.of(contractDTO));

        Contract retrieved = contractService.getContractByContractId(42L);


        assertEquals("Test Contract", retrieved.getContractName());
        assertEquals("NATTE", retrieved.getContractNumber());
    }
}
