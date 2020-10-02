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

import static org.junit.Assert.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
class CoverageSearchTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private SponsorRepository sponsorRepo;

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepository;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private DataSetup dataSetup;

    private Sponsor sponsor;
    private Contract contract1;
    private Contract contract2;


    @AfterEach
    void after() {
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepository.deleteAll();
        coveragePeriodRepo.deleteAll();

        contractRepo.delete(contract1);
        contractRepo.delete(contract2);
        contractRepo.flush();

        sponsorRepo.delete(sponsor);
        sponsorRepo.flush();
    }

    @Test
    void testSearches() {
        try {

            sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
            contract1 = dataSetup.setupContract(sponsor, "c123");
            contract2 = dataSetup.setupContract(sponsor, "c456");

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
