package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Since this test modifies properties
 */

@SpringBootTest
@TestPropertySource(locations = "/application.properties")
@Testcontainers
@TestPropertySource(properties = {
        "hpms.base.path=invalid",
        "hpms.base.url=localhost"
})
class HPMSFetcherExceptionIT {

    @Autowired
    private HPMSFetcher fetcher;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    void invalidSponsorUrl() {
        Assertions.assertThrows(WebClientRequestException.class, () -> {
            fetcher.retrieveSponsorInfo();
        });
    }

    @Test
    void invalidAttestationUrl() {
        Assertions.assertThrows(WebClientRequestException.class, () -> {
            fetcher.retrieveAttestationInfo(null);
        });
    }
}
