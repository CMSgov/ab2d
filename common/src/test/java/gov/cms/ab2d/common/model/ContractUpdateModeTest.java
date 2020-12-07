package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    private SponsorRepository sponsorRepository;

    @Test
    void testAutomaticUpdate() {
        Contract contract = buildContract();
        assertFalse(contract.hasAttestation());
        contract.updateAttestation(true, TEST_DATE_STR);
        assertTrue(contract.hasAttestation());

        cleanup(contract);
    }

    @Test
    void testManualOverride() {
        Contract contract = buildContract();
        assertFalse(contract.hasAttestation());
        contract.setUpdateMode(Contract.UpdateMode.MANUAL);
        contract.updateAttestation(true, TEST_DATE_STR);
        assertFalse(contract.hasAttestation());

        cleanup(contract);
    }

    private void cleanup(Contract contract) {
        Sponsor sponsor = contract.getSponsor();
        // Cleanup
        contractRepository.delete(contract);
        sponsorRepository.delete(sponsor);
    }

    private Contract buildContract() {
        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(52);
        sponsor.setOrgName("TEST");
        Sponsor savedSponsor = sponsorRepository.save(sponsor);

        return contractRepository.save(new Contract("Z1234", "Test Contract",
                9999L, "Test Parent Org",
                "Test Parent Org Marketing Name", savedSponsor));
    }
}
