package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSAuthResponse;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import org.mockito.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Service
public class MockWebClient {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec headersSpec;

    @Mock
    WebClient.RequestHeadersUriSpec headersUriSpec;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Mock
    ClientResponse response;


    public void authRequest(WebClient mockedWebClient, MockedStatic<WebClient> webClientStatic, HPMSAuthResponse body) {
        mock(mockedWebClient, webClientStatic);
        LinkedMultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
        cookies.add("test", ResponseCookie.fromClientResponse("test", "test").build());
        Mockito.when(response.cookies()).thenReturn(cookies);
        Mockito.when(response.bodyToMono(HPMSAuthResponse.class)).thenReturn(Mono.just(body));
        ArgumentCaptor<Function<ClientResponse, ? extends Mono<HPMSAuthResponse>>> argument = ArgumentCaptor.forClass(Function.class);
        when(headersSpec.exchangeToMono(argument.capture())).thenAnswer(x -> argument.getValue().apply(response));

    }

    public void orgRequest(WebClient mockedWebClient, MockedStatic<WebClient> webClientStatic, List<HPMSOrganizationInfo> body) {
        mock(mockedWebClient, webClientStatic);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(body));
    }


    public void attestationRequest(WebClient mockedWebClient, MockedStatic<WebClient> webClientStatic, Set<HPMSAttestation> body) {
        mock(mockedWebClient, webClientStatic);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(body));
    }

    public void mock(WebClient mockedWebClient, MockedStatic<WebClient> webClientStatic) {
        MockitoAnnotations.openMocks(this);
        Mockito.when(webClientBuilder.exchangeStrategies(Mockito.any(ExchangeStrategies.class))).thenReturn(webClientBuilder);
        Mockito.when(webClientBuilder.clientConnector(Mockito.any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        Mockito.when(webClientBuilder.build()).thenReturn(mockedWebClient);
        webClientStatic.when(WebClient::create).thenReturn(mockedWebClient);
        when(mockedWebClient.post()).thenReturn(requestBodyUriSpec);
        when(mockedWebClient.get()).thenReturn(headersUriSpec);
        when(mockedWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(headersUriSpec.uri(any(URI.class))).thenReturn(headersUriSpec);
        when(headersUriSpec.headers(any())).thenReturn(headersUriSpec);
        when(headersUriSpec.retrieve()).thenReturn(responseSpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.accept(any())).thenReturn(headersSpec);

    }
}
