package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.repository.CoverageDeltaRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
public class CoverageDeltaTest {

    @Autowired
    CoverageDeltaRepository cdr;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    public void testEmpty() {
        CoverageSearchEvent coverageSearchEvent1 = new CoverageSearchEvent();
        coverageSearchEvent1.setId(-1L);
        CoverageSearchEvent coverageSearchEvent2 = new CoverageSearchEvent();
        coverageSearchEvent2.setId(-2L);
        cdr.trackDeltas(coverageSearchEvent1, coverageSearchEvent2);
    }
}
