package gov.cms.ab2d.common.service;


import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.SponsorIPDTO;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class SponsorIPServiceTest {

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SponsorIPService sponsorIPService;

    @Autowired
    private DataSetup dataSetup;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
        sponsorRepository.deleteAll();
    }

    @Test
    public void testSponsorIPs() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);
        SponsorDTO sponsorDTO = new SponsorDTO(sponsor.getParent().getHpmsId(), sponsor.getParent().getOrgName());
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO(sponsorDTO, Set.of("127.0.0.1", "44.45.45.44"));
        SponsorIPDTO sponsorIPDTOUpdate = sponsorIPService.addIPAddress(sponsorIPDTO);

        Assert.assertEquals(sponsorIPDTO, sponsorIPDTOUpdate);

        SponsorIPDTO sponsorIPDTODeleted = sponsorIPService.removeIPAddresses(sponsorIPDTOUpdate);

        Assert.assertEquals(Set.of(), sponsorIPDTODeleted.getIps());

        sponsorIPDTO.setIps(Set.of("33.33.33.33"));

        SponsorIPDTO sponsorIPDTOAdded = sponsorIPService.addIPAddress(sponsorIPDTO);

        Assert.assertEquals(Set.of("33.33.33.33"), sponsorIPDTOAdded.getIps());

        SponsorIPDTO sponsorIPDTORetrieved = sponsorIPService.getIPs(sponsorIPDTOAdded.getSponsor());

        Assert.assertEquals(Set.of("33.33.33.33"), sponsorIPDTORetrieved.getIps());
    }

    @Test
    public void testSponsorIPsEmpty() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);
        SponsorDTO sponsorDTO = new SponsorDTO(sponsor.getParent().getHpmsId(), sponsor.getParent().getOrgName());
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO(sponsorDTO, Set.of());

        var exceptionThrown = Assertions.assertThrows(InvalidUserInputException.class, () -> {
            sponsorIPService.addIPAddress(sponsorIPDTO);
        });
        assertEquals("IPs cannot be empty", exceptionThrown.getMessage());
    }

    @Test
    public void testSponsorEmpty() {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO(null, Set.of("12.1.12.1"));

        var exceptionThrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            sponsorIPService.addIPAddress(sponsorIPDTO);
        });
        assertEquals("source cannot be null", exceptionThrown.getMessage());
    }

    @Test
    public void testSponsorInvalid() {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO(new SponsorDTO(123, "Bad Corp"), Set.of("12.1.12.1"));

        var exceptionThrown = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sponsorIPService.addIPAddress(sponsorIPDTO);
        });
        assertEquals("No sponsor found with hpms ID 123 and org name Bad Corp", exceptionThrown.getMessage());
    }

    @Test
    public void testSponsorInvalidIP() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);
        SponsorDTO sponsorDTO = new SponsorDTO(sponsor.getParent().getHpmsId(), sponsor.getParent().getOrgName());
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO(sponsorDTO, Set.of("wefjklw342423"));

        var exceptionThrown = Assertions.assertThrows(InvalidUserInputException.class, () -> {
            sponsorIPService.addIPAddress(sponsorIPDTO);
        });
        assertEquals("IP provided wefjklw342423 is not valid", exceptionThrown.getMessage());
    }
}
