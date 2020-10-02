package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.Assert.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CoverageSearchTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private CoverageSearchRepository coverageSearchRepository;

    @Test
    void testSearches() {
        try {
            String contract1 = "c123";
            String contract2 = "c456";
            CoverageSearch search1 = new CoverageSearch(null,
                    contract1, 10, 2020, OffsetDateTime.now());
            CoverageSearch search2 = new CoverageSearch(null,
                    contract1, 10, 2020, OffsetDateTime.now().minusDays(1));
            CoverageSearch search3 = new CoverageSearch(null,
                    contract2, 10, 2020, OffsetDateTime.now().minusDays(2));
            CoverageSearch savedSearch1 = coverageSearchRepository.save(search1);
            CoverageSearch savedSearch2 = coverageSearchRepository.save(search2);
            CoverageSearch savedSearch3 = coverageSearchRepository.save(search3);

            Optional<CoverageSearch> searchedSearch3Optional = coverageSearchRepository.findFirstByOrderByCreatedDesc();
            assertTrue(searchedSearch3Optional.isPresent());
            CoverageSearch searchedSearch3 = searchedSearch3Optional.get();
            assertEquals(savedSearch3, searchedSearch3);
            Optional<CoverageSearch> searchedSearch2Optional = coverageSearchRepository.findFirstByContractOrderByCreatedDesc(contract1);
            assertTrue(searchedSearch2Optional.isPresent());
            CoverageSearch searchedSearch2 = searchedSearch2Optional.get();
            assertEquals(savedSearch2, searchedSearch2);
        } finally {
            // Cleanup
            coverageSearchRepository.deleteAll();
        }
    }
}
