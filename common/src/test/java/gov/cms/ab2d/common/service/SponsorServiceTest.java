package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
public class SponsorServiceTest {

    @Autowired
    private SponsorService sponsorService;


    @Autowired
    private ContractService contractService;


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
