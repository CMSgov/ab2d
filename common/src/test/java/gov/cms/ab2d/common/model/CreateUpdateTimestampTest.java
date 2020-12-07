package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.Assert.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CreateUpdateTimestampTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepository;

    @Test
    void testTimestamps() {
        CoverageSearchEvent coverageSearchEvent = new CoverageSearchEvent();
        coverageSearchEvent.setDescription("TEST");

        assertNull(coverageSearchEvent.getCreated());
        assertNull(coverageSearchEvent.getModified());

        CoverageSearchEvent savedCSE = coverageSearchEventRepository.save(coverageSearchEvent);
        assertEquals("TEST", savedCSE.getDescription());
        assertNotNull(savedCSE.getId());
        assertNotNull(savedCSE.getCreated());
        assertNotNull(savedCSE.getModified());

        OffsetDateTime created = savedCSE.getCreated();
        OffsetDateTime modified = savedCSE.getModified();
        savedCSE.setDescription("TEST2");
        CoverageSearchEvent finaleCSE = coverageSearchEventRepository.save(savedCSE);

        assertEquals(created, finaleCSE.getCreated());
        assertNotEquals(modified, finaleCSE.getModified());

        // Cleanup
        coverageSearchEventRepository.delete(finaleCSE);
    }
}
