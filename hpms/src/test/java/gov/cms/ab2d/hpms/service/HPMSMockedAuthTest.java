package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.LogManager;
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
import org.springframework.http.HttpStatus;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

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
    private LogManager eventLogger;

    @MockBean
    private WebClient mockedWebClient;

    @Test
    void auth() {
        HPMSAuthResponse hpmsAuthResponse = new HPMSAuthResponse();
        hpmsAuthResponse.setAccessToken("TOKEN");
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequestError(mockedWebClient, webClientStatic, OK, new HPMSAuthResponse());
            HttpHeaders headers = new HttpHeaders();
            authService.buildAuthHeaders(headers);
            assertNotNull(headers.get(COOKIE));
            assertTrue(headers.get(COOKIE).stream().anyMatch(cookie -> cookie.contains("test")));
        }
    }

    @Test
    void authInvalidKey() {
        clientTest(INTERNAL_SERVER_ERROR);
    }

    @Test
    void authExpiredKey() {
        clientTest(FORBIDDEN);
    }

    @Test
    void authUnknownError() {
        clientTest(REQUEST_TIMEOUT);
    }

    @Test
    void authTimeoutException() {
        serviceDownTest(false);
    }

    @Test
    void authReturnsNull() {
        serviceDownTest(true);
    }

    private void serviceDownTest(boolean nullBody) {
        HttpHeaders headers = new HttpHeaders();
        // This is to test when HMPS is completely down so we don't get any response.
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequestTimeout(mockedWebClient, webClientStatic, nullBody);
            assertThrows(RemoteTimeoutException.class, () -> authService.buildAuthHeaders(headers));
        }
    }

    private void clientTest(HttpStatus httpStatus) {
        HttpHeaders headers = new HttpHeaders();
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequestError(mockedWebClient, webClientStatic, httpStatus, new HPMSAuthResponse());
            assertThrows(WebClientResponseException.class, () -> authService.buildAuthHeaders(headers));
            verify(eventLogger, times(1)).log(eq(EventClient.LogType.SQL), any(LoggableEvent.class));
        }
    }

    @AfterEach
    public void shutdown() {
        Mockito.reset(mockedWebClient);
        authService.cleanup();
    }
}
