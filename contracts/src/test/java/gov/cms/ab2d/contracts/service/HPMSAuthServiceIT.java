package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "/application.properties")
@Testcontainers
// HPMSAuthServiceImpl holds some state that's useful normally but can break tests. Reset the bean after each test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HPMSAuthServiceIT {

    @Autowired
    HPMSAuthServiceImpl authService;


    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void tokenTest() {
        assertNotNull(authService);

        // Verifying initial state
        assertTrue(authService.getAuthToken().isBlank());
        assertEquals(0L, authService.getTokenRefreshAfter());

        // Verifying the first time
        HttpHeaders headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        //noinspection ConstantConditions
        assertEquals(authService.getAuthToken(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
        assertNotEquals(0, authService.getTokenRefreshAfter());

        String firstAuthToken = authService.getAuthToken();
        // Second call should return identical token (since it is cached)
        headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        //noinspection ConstantConditions
        assertEquals(firstAuthToken, headers.get(HttpHeaders.AUTHORIZATION).get(0));
        long tokenExpiry = authService.getTokenRefreshAfter();

        // Force an expiry and see a new token is retrieved, can't depend upon the actual token being physically
        // refreshed (without inserting a 500 ms pause), so just check expiry hear (which with 1 clock tick, will be
        // different.
        authService.clearToken();
        headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        assertNotEquals(tokenExpiry, authService.getTokenRefreshAfter());
    }

    @Test
    void headers() {
        HttpHeaders headers = new HttpHeaders();
        authService.buildAuthHeaders(headers);
        // Method currently sets 3 headers
        assertTrue(headers.size() >= 3);
        headers.forEach((headerName, headerValue) -> {
            assertNotNull(headerValue);
        });
    }

    @AfterEach
    public void shutdown() {
        authService.cleanup();
    }
}
