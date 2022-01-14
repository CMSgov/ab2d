package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
public class HPMSAuthServiceTest {

    @Autowired
    HPMSAuthServiceImpl authService;

    @Autowired
    HPMSFetcherImpl hpmsFetcher;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void tokenTest() {
        assertNotNull(authService);

        // Verifying initial state
        assertNull(authService.getAuthToken());
        assertEquals(0L, authService.getTokenExpires());

        // Verifying the first time
        HttpHeaders headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        //noinspection ConstantConditions
        assertEquals(authService.getAuthToken(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
        assertNotEquals(0, authService.getTokenExpires());

        String firstAuthToken = authService.getAuthToken();
        // Second call should return identical token (since it is cached)
        headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        //noinspection ConstantConditions
        assertEquals(firstAuthToken, headers.get(HttpHeaders.AUTHORIZATION).get(0));
        long tokenExpiry = authService.getTokenExpires();

        // Force an expiry and see a new token is retrieved, can't depend upon the actual token being physically
        // refreshed (without inserting a 500 ms pause), so just check expiry hear (which with 1 clock tick, will be
        // different.
        authService.clearTokenExpires();
        headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        assertNotEquals(tokenExpiry, authService.getTokenExpires());
    }

    @AfterEach
    public void shutdown() {
        authService.cleanup();
    }
}
