package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAuthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
            client.authRequest(mockedWebClient, webClientStatic, OK, hpmsAuthResponse);
            HttpHeaders headers = new HttpHeaders();
            authService.buildAuthHeaders(headers);
            assertNotNull(headers.get(COOKIE));
            assertTrue(headers.get(COOKIE).stream().anyMatch(cookie -> cookie.contains("test")));
        }
    }

    @Test
    void authNoKey() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequest(mockedWebClient, webClientStatic, INTERNAL_SERVER_ERROR, new HPMSAuthResponse());
            assertThrows(RemoteTimeoutException.class, () -> authService.buildAuthHeaders(new HttpHeaders()));
            verify(eventLogger, times(1)).log(any(LoggableEvent.class));
        }
    }

    @Test
    void authInvalidKey() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequest(mockedWebClient, webClientStatic, FORBIDDEN, new HPMSAuthResponse());
            assertThrows(RemoteTimeoutException.class, () -> authService.buildAuthHeaders(new HttpHeaders()));
            verify(eventLogger, times(1)).log(any(LoggableEvent.class));
        }
    }

    @Test
    void authUnknownError() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.authRequest(mockedWebClient, webClientStatic, REQUEST_TIMEOUT, new HPMSAuthResponse());
            assertThrows(RemoteTimeoutException.class, () -> authService.buildAuthHeaders(new HttpHeaders()));
            verify(eventLogger, times(1)).log(any(LoggableEvent.class));
        }
    }

    @AfterEach
    public void shutdown() {
        Mockito.reset(mockedWebClient);
        authService.cleanup();
    }
}
