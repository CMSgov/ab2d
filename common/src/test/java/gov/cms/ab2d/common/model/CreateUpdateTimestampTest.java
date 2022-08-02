package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CreateUpdateTimestampTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private ContractRepository contractRepository;

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
    }

    @Test
    void testTimestamps() {
        Contract contract = new Contract();
        contract.setContractNumber("TEST123");
        contract.setContractName("TEST123");

        assertNull(contract.getCreated());
        assertNull(contract.getModified());

        Contract savedCSE = contractRepository.save(contract);
        dataSetup.queueForCleanup(savedCSE);

        assertEquals("TEST123", savedCSE.getContractNumber());
        assertNotNull(savedCSE.getId());
        assertNotNull(savedCSE.getCreated());
        assertNotNull(savedCSE.getModified());

        OffsetDateTime created = savedCSE.getCreated();
        OffsetDateTime modified = savedCSE.getModified();
        contract.setContractNumber("TEST456");
        contract.setContractName("TEST456");
        Contract finaleCSE = contractRepository.save(savedCSE);

        assertEquals(created, finaleCSE.getCreated());
        assertNotEquals(modified, finaleCSE.getModified());
    }
}
