package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.SpringBootTestApp;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
class HPMSMockedAuthTest {

    @Autowired
    HPMSAuthServiceImpl authService;

    @MockBean
    private WebClient.Builder webClientBuilder;

    @MockBean
    private WebClient mockedWebClient;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec headersSpec;


    @Mock
    MediaType mediaType;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;
    @Mock
    Flux flux;

    @Mock
    WebClient.ResponseSpec responseSpec;


    @Test
    void cookies() {
        Mockito.when(webClientBuilder.exchangeStrategies(Mockito.any(ExchangeStrategies.class))).thenReturn(webClientBuilder);
        Mockito.when(webClientBuilder.clientConnector(Mockito.any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        Mockito.when(webClientBuilder.build()).thenReturn(mockedWebClient);
        final MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class);
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
        assertThrows(RuntimeException.class, () -> {
            authService.buildAuthHeaders(new HttpHeaders());
        });
    }
}
