package gov.cms.ab2d.hpms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class HPMSMockedAuthTest {

    HPMSAuthServiceImpl authService = new HPMSAuthServiceImpl();

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient mockedWebClient;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec headersSpec;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;

    @Mock
    Flux flux;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @BeforeEach
    public void initMock() {
        //let's not drag the spring context into this
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void cookies() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            Mockito.when(webClientBuilder.exchangeStrategies(Mockito.any(ExchangeStrategies.class))).thenReturn(webClientBuilder);
            Mockito.when(webClientBuilder.clientConnector(Mockito.any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
            Mockito.when(webClientBuilder.build()).thenReturn(mockedWebClient);
            webClientStatic.when(WebClient::create).thenReturn(mockedWebClient);
            when(mockedWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
            when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(headersSpec);
            when(headersSpec.accept(any())).thenReturn(headersSpec);
            when(headersSpec.exchangeToFlux(any())).thenReturn(flux);
            when(responseSpec.bodyToFlux(ArgumentMatchers.<Class<String>>notNull()))
                    .thenReturn(Flux.just("resp"));
            HttpHeaders headers = new HttpHeaders();
            assertThrows(RuntimeException.class, () -> {
                authService.buildAuthHeaders(headers);
            });
        }
    }
}
