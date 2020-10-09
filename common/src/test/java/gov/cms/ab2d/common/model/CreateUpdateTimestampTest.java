package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CreateUpdateTimestampTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private SponsorRepository sponsorRepository;

    @Test
    void testTimestamps() {
        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(52);
        sponsor.setOrgName("TEST");

        assertNotNull(sponsor.getHpmsId());
        assertNotNull(sponsor.getOrgName());
        assertNull(sponsor.getCreated());
        assertNull(sponsor.getModified());

        Sponsor savedSponsor = sponsorRepository.save(sponsor);
        assertEquals("TEST", savedSponsor.getOrgName());
        assertNotNull(savedSponsor.getId());
        assertNotNull(savedSponsor.getCreated());
        assertNotNull(savedSponsor.getModified());

        OffsetDateTime created = savedSponsor.getCreated();
        OffsetDateTime modified = savedSponsor.getModified();
        savedSponsor.setOrgName("TEST2");
        Sponsor finaleSponsor = sponsorRepository.save(savedSponsor);
        assertEquals(created, finaleSponsor.getCreated());
        assertNotEquals(modified, finaleSponsor.getModified());

        // Cleanup
        sponsorRepository.delete(finaleSponsor);
    }
}
