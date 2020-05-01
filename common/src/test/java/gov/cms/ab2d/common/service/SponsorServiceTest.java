package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class SponsorServiceTest {

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
        sponsorRepository.deleteAll();
    }

    @Test
    public void testSponsors() {
        Sponsor parent = new Sponsor();
        parent.setHpmsId(456);
        parent.setOrgName("Parent Corp.");
        parent.setLegalName("Parent Corp.");

        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(123);
        sponsor.setOrgName("Health Ins.");
        sponsor.setLegalName("Health Ins.");
        sponsor.setParent(parent);

        Contract contract = new Contract();
        contract.setContractName("Health Ins. Agreement");
        contract.setContractNumber("S1234");
        contract.setAttestedOn(OffsetDateTime.now());

        sponsor.getContracts().add(contract);
        contract.setSponsor(sponsor);

        Sponsor savedSponsor = sponsorService.saveSponsor(sponsor);

        Assert.assertEquals(parent, savedSponsor.getParent());

        Assert.assertTrue(savedSponsor.hasContract("S1234"));

        Assert.assertEquals("Health Ins.", sponsor.getOrgName());
        Assert.assertEquals("Health Ins.", sponsor.getLegalName());
        Assert.assertEquals(Integer.valueOf(123), sponsor.getHpmsId());

        Optional<Contract> retrievedContractOptional =
                contractService.getContractByContractNumber("S1234");
        Contract retrievedContract = retrievedContractOptional.get();
        Assert.assertEquals(retrievedContract.getContractNumber(), contract.getContractNumber());
        Assert.assertEquals(retrievedContract.getContractName(), contract.getContractName());
    }
}
