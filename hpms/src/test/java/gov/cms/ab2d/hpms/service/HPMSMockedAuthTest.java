package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAuthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.COOKIE;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
class HPMSMockedAuthTest {

    private final MockWebClient client = new MockWebClient();

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    HPMSAuthServiceImpl authService;
    @Autowired
    MockWebClient mockWebClient;

    @MockBean
    private WebClient mockedWebClient;

    @Test
    void auth() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequest(mockedWebClient, webClientStatic, new HPMSAuthResponse());
            HttpHeaders headers = new HttpHeaders();
            authService.buildAuthHeaders(headers);
            assertNotNull(headers.get(COOKIE));
            assertTrue(headers.get(COOKIE).stream().anyMatch(cookie -> cookie.contains("test")));
        }
    }

    @AfterEach
    public void shutdown() {
        authService.cleanup();
    }
}
