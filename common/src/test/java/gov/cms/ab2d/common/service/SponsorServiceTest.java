package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.Attestation;
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
@TestPropertySource(locations = "/common-it.properties")
public class SponsorServiceTest {

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private AttestationService attestationService;

    @Autowired
    private ContractService contractService;

    @Test
    public void testSponsors() {
        Sponsor parent = new Sponsor();
        parent.setHpmsId(456);
        parent.setOrgName("Parent Corp.");
        parent.setLegalName("Parent Corp.");

        Sponsor savedParent = sponsorService.saveSponsor(parent);

        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(123);
        sponsor.setOrgName("Health Ins.");
        sponsor.setLegalName("Health Ins.");
        sponsor.setParent(savedParent);

        Contract contract = new Contract();
        contract.setContractName("Health Ins. Agreement");
        contract.setContractId("S1234");

        Attestation attestation = new Attestation();
        attestation.setAttestedOn(OffsetDateTime.now());
        attestation.setSponsor(sponsor);
        attestation.setContract(contract);

        contract.getAttestations().add(attestation);

        sponsor.getAttestations().add(attestation);

        Sponsor savedSponsor = sponsorService.saveSponsor(sponsor);

        Assert.assertEquals(savedParent, savedSponsor.getParent());

        Assert.assertTrue(savedSponsor.hasContract("S1234"));

        Assert.assertEquals(contract, sponsor.getAttestations().iterator().next().getContract());

        Assert.assertEquals("Health Ins.", sponsor.getOrgName());
        Assert.assertEquals("Health Ins.", sponsor.getLegalName());
        Assert.assertEquals(Integer.valueOf(123), sponsor.getHpmsId());

        Attestation retrievedAttestation = attestationService.getMostRecentAttestationFromContract(contract);
        Assert.assertEquals(attestation.getAttestedOn(), retrievedAttestation.getAttestedOn());

        OffsetDateTime pastDate = OffsetDateTime.now().minusHours(36);
        retrievedAttestation.setAttestedOn(pastDate);
        Attestation updatedAttestation = attestationService.saveAttestation(retrievedAttestation);
        Assert.assertEquals(updatedAttestation.getAttestedOn(), pastDate);

        Optional<Contract> retrievedContractOptional = contractService.getContractByContractId("S1234");
        Contract retrievedContract = retrievedContractOptional.get();
        Assert.assertEquals(retrievedContract.getContractId(), contract.getContractId());
        Assert.assertEquals(retrievedContract.getContractName(), contract.getContractName());
    }
}
