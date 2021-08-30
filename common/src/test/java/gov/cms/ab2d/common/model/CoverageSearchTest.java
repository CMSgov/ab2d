package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.*;
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
import java.util.Optional;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CoverageSearchTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private CoverageSearchRepository coverageSearchRepository;

    @Autowired
    private DataSetup dataSetup;

    @AfterEach
    void cleanup() {
        dataSetup.cleanup();
    }

    @Test
    void testSearches() {
        try {

            Contract contract1 = dataSetup.setupContract("c123", AB2D_EPOCH.toOffsetDateTime());
            Contract contract2 = dataSetup.setupContract("c456", AB2D_EPOCH.toOffsetDateTime());

            CoveragePeriod period1 = dataSetup.createCoveragePeriod(contract1, 10, 2020);
            CoveragePeriod period2 = dataSetup.createCoveragePeriod(contract2, 10, 2020);

            CoverageSearch search1 = new CoverageSearch(null, period1, OffsetDateTime.now(), 0);
            CoverageSearch search2 = new CoverageSearch(null, period2, OffsetDateTime.now().minusDays(2), 0);

            CoverageSearch savedSearch1 = coverageSearchRepository.save(search1);

            CoverageSearch savedSearch2 = coverageSearchRepository.save(search2);

            Optional<CoverageSearch> searchedSearch2Optional = coverageSearchRepository.findFirstByOrderByCreatedAsc();
            assertTrue(searchedSearch2Optional.isPresent());
            CoverageSearch searchedSearch2 = searchedSearch2Optional.get();
            assertEquals(savedSearch2, searchedSearch2);
        } finally {
            // Cleanup
            coverageSearchRepository.deleteAll();
        }
    }
}
