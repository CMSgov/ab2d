package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.ContractRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class ContractUpdateModeTest {

    private static final String TEST_DATE_STR = "2020-04-15 14:57:34";

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private DataSetup dataSetup;

    private Contract contract;

    @BeforeEach
    public void before() {
        contract = buildContract();
        dataSetup.queueForCleanup(contract);
    }

    @AfterEach
    public void after() {
        dataSetup.cleanup();
    }

    @Test
    void testAutomaticUpdate() {
        assertFalse(contract.hasAttestation());
        contract.updateAttestation(true, TEST_DATE_STR);
        assertTrue(contract.hasAttestation());
    }

    @Test
    void testManualOverride() {
        assertFalse(contract.hasAttestation());
        contract.setUpdateMode(Contract.UpdateMode.MANUAL);
        contract.updateAttestation(true, TEST_DATE_STR);
        assertFalse(contract.hasAttestation());
    }

    private Contract buildContract() {
        return contractRepository.save(new Contract("Z1234", "Test Contract",
                9999L, "Test Parent Org",
                "Test Parent Org Marketing Name"));
    }
}
