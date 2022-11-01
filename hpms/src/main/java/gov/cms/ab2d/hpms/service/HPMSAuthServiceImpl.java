package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.hpms.hmsapi.HPMSAuthResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;


import static gov.cms.ab2d.eventclient.events.ErrorEvent.ErrorType.HPMS_AUTH_ERROR;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpStatus.OK;

@Service
public class HPMSAuthServiceImpl extends AbstractHPMSService implements HPMSAuthService {

    private static final String HPMS_ORGANIZATION = "HPMS_AUTH";

    @Value("${hpms.base.url}/api/idm/OAuth/AMMtoken")
    private String authURL;

    @Value("${HPMS_AUTH_KEY_ID}")
    private String hpmsAPIKeyId;

    @Value("${HPMS_AUTH_KEY_SECRET}")
    private String hpmsSecret;

    @Autowired
    private WebClient webClient;

    @Autowired
    private SQSEventClient eventLogger;

    private URI fullAuthURI;

    private volatile String authToken;
    private volatile String cookies;

    private volatile long tokenExpires;

    @PostConstruct
    private void buildFullAuthURI() {
        fullAuthURI = buildFullURI(authURL);
    }

    @Override
    public void buildAuthHeaders(HttpHeaders headers) {
        headers.set("X-API-CONSUMER-ID", hpmsAPIKeyId);
        headers.set(AUTHORIZATION, retrieveAuthToken());
        // re-injecting cookies using WebClient's cookie handler is even more cumbersome
        headers.set(COOKIE, cookies);
    }

    private String retrieveAuthToken() {
        final long currentTimestamp = System.currentTimeMillis();
        if (authToken == null || currentTimestamp >= tokenExpires) {

            refreshToken(currentTimestamp);
        }

        return authToken;
    }

    private void refreshToken(long currentTimestamp) {
        authToken = null;

        Mono<HPMSAuthResponse> orgInfoMono = webClient
                .post().uri(fullAuthURI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(retrieveAuthRequestPayload())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    cookies = extractCookies(response.cookies());
                    return response.statusCode().equals(OK)
                            ? response.bodyToMono(HPMSAuthResponse.class)
                            : response.createException().flatMap(Mono::error);
                });

        long curTime = System.currentTimeMillis();
        HPMSAuthResponse authResponse;
        try {
            // Cough up blood if we can't get an Auth response in a minute.
            authResponse = Optional.ofNullable(orgInfoMono.block(Duration.ofMinutes(1)))
                    .orElseThrow(IllegalStateException::new);

            // Convert seconds to millis at a 90% level to pad refreshing of a token so that we are not in the middle of
            // a significant operation when the token expires.
            tokenExpires = currentTimestamp + authResponse.getExpires() * 900L;
            authToken = authResponse.getAccessToken();
        } catch (WebClientResponseException exception) {
            eventLogger.log(EventClient.LogType.SQL,
                    new ErrorEvent(HPMS_ORGANIZATION, "", HPMS_AUTH_ERROR, prepareErrorMessage(exception, curTime)));
            throw exception;
        } catch (IllegalStateException | NullPointerException exception) {
            String message = "HPMS auth call failed with no response waited for " + (curTime / 1000) + " seconds.";
            eventLogger.log(EventClient.LogType.SQL,
                    new ErrorEvent(HPMS_ORGANIZATION, "", HPMS_AUTH_ERROR, message));
            throw new RemoteTimeoutException(message);
        }
    }

    private String extractCookies(MultiValueMap<String, ResponseCookie> entries) {
        return entries.entrySet().stream()
                .map(cookie -> cookie.getValue()
                        .stream()
                        .map(responseCookie -> cookie.getKey() + "=" + responseCookie.getValue()))
                .flatMap(Stream::sorted)
                .collect(Collectors.joining("; "));
    }

    private String retrieveAuthRequestPayload() {
        return String.format(AUTH_PAYLOAD_TEMPLATE, hpmsAPIKeyId, hpmsSecret);
    }

    // Text Blocks are still a preview feature in JDK 13 and 14, otherwise, it would really clean this up.
    private static final String AUTH_PAYLOAD_TEMPLATE = "{\n" +
            "    \"userName\": \"CDA-AB2D-API\",\n" +
            "    \"keyId\": \"%s\",\n" +
            "    \"keySecret\": \"%s\",\n" +
            "    \"scopes\": \"cda_org_att\"\n" +
            "}";


    String getAuthToken() {
        return authToken;
    }

    long getTokenExpires() {
        return tokenExpires;
    }

    void clearTokenExpires() {
        this.tokenExpires = 0;
    }

    void cleanup() {
        clearTokenExpires();
        authToken = null;
    }

    private String prepareErrorMessage(WebClientResponseException exception, long curTime) {
        String explication = "After waiting " + (curTime / 1000) + " seconds ";
        switch (exception.getStatusCode().value()) {
            //TODO Replace status code numbers with HttpStatus emums
            case (403):
                explication += "HPMS auth key/secret have expired and must be updated";
                break;
            case (500):
                explication += "HPMS auth key/secret are invalid or HPMS is down";
                break;
            default:
                explication += "HPMS returned an unknown error: "
                        + exception.getResponseBodyAsString();
        }

        return explication;
    }
}
