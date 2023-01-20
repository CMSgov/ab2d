package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.util.AB2DCoverageLocalstackContainer;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.coverage.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.coverage.repository"})
@TestPropertySource(locations = "/application.coverage.properties")
@EnableFeignClients(clients = {ContractFeignClient.class})
class CoverageSearchTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DCoveragePostgressqlContainer();

    @Container
    private static final AB2DCoverageLocalstackContainer localstackContainer = new AB2DCoverageLocalstackContainer();

    @Autowired
    private CoverageSearchRepository coverageSearchRepository;

    @Autowired
    private CoverageDataSetup coverageDataSetup;

    @AfterEach
    void cleanup() {
        coverageDataSetup.cleanup();
    }

    @Test
    void testSearches() {
        try {

            CoveragePeriod period1 = coverageDataSetup.createCoveragePeriod("c123", 10, 2020);
            CoveragePeriod period2 = coverageDataSetup.createCoveragePeriod("c456", 10, 2020);

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
