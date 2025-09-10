package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.controller.InvalidContractException;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.contracts.repository.ContractRepository;
import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
class ContractServiceTest {
    @Autowired
    private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();

    private ContractService service;

    @BeforeEach
    void init() {
        service = new ContractServiceImpl(contractRepository);
        contractRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        contractRepository.deleteAll();
    }

    @Test
    void testTheService() {
        List<ContractDTO> contracts = service.getAllContracts();
        assertNotNull(contracts);
        assertEquals(0, contracts.size());
        Contract originalContract = new Contract("Z0000", "test", 00l, "ORG", "Marketing", 100, 95);
        contractRepository.save(originalContract);

        contracts = service.getAllContracts();
        assertNotNull(contracts);
        assertEquals(0, contracts.size());

        originalContract.setAttestedOn(OffsetDateTime.now());
        service.updateContract(originalContract.toDTO());

        contracts = service.getAllContracts();
        assertEquals(1, contracts.size());

        Contract contract = service.getContractByContractId(originalContract.getId());
        assertEquals(originalContract, contract);
        contract = service.getContractByContractNumber("Z0000");
        assertEquals(originalContract, contract);
    }

    @Test
    void testInvalidContract() {
        InvalidContractException exception = assertThrows(InvalidContractException.class, () -> {
            service.getContractByContractId(42L);
        });
        assertEquals("Invalid Contract Given", exception.getMessage());

        exception = assertThrows(InvalidContractException.class, () -> {
            service.getContractByContractNumber("");
        });
        assertEquals("Invalid Contract Given", exception.getMessage());
    }
}
