package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.hpms.hmsapi.HPMSAuthResponse;
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

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.common.util.Constants.HPMS_AUTHORIZATION;
import static gov.cms.ab2d.eventlogger.events.ErrorEvent.ErrorType.HpMS_AUTH_ERROR;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpStatus.OK;

@Service
public class HPMSAuthServiceImpl extends AbstractHPMSService implements HPMSAuthService {

    @Value("${hpms.base.url}/api/idm/OAuth/AMMtoken")
    private String authURL;

    @Value("${HPMS_AUTH_KEY_ID}")
    private String hpmsAPIKeyId;

    @Value("${HPMS_AUTH_KEY_SECRET}")
    private String hpmsSecret;

    @Autowired
    private WebClient webClient;

    @Autowired
    private LogManager eventLogger;

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
                    if (response.statusCode().equals(OK)) {
                        cookies = extractCookies(response.cookies());
                        return response.bodyToMono(HPMSAuthResponse.class);
                    } else {
                        return response.createException().flatMap(Mono::error);
                    }
                });

        // Cough up blood if we can't get an Auth response in a minute.
        long curTime = System.currentTimeMillis();
        HPMSAuthResponse authResponse;
        try {
            authResponse = orgInfoMono.block(Duration.ofMinutes(1));
            if (authResponse == null) {
                logErrors("HPMS auth call failed with no response", curTime);
                return;
            }
            // Convert seconds to millis at a 90% level to pad refreshing of a token so that we are not in the middle of
            // a significant operation when the token expires.
            tokenExpires = currentTimestamp + authResponse.getExpires() * 900L;
            authToken = authResponse.getAccessToken();
        } catch (WebClientResponseException exception) {
            logErrors(prepareErrorMessage(exception), curTime);
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

    private void logErrors(String errorMessage, long curTime) {
        eventLogger.log(new ErrorEvent(HPMS_AUTHORIZATION, "", HpMS_AUTH_ERROR, errorMessage));
        long elapsedTime = System.currentTimeMillis() - curTime;
        throw new RemoteTimeoutException("Failed to procure Auth Token, response: " + errorMessage +
                " waited for " + (elapsedTime / 1000) + " seconds.");
    }

    private String prepareErrorMessage(WebClientResponseException exception) {
        String explication;
        switch (exception.getStatusCode().value()) {
            case (403) -> {
                explication = "HPMS auth key/secret have expired and must be updated";
            }
            case (500) -> {
                explication = "HPMS auth key/secret are invalid or HPMS is down";
            }
            default -> {
                explication = "HPMS returned an unknown error" + ": "
                        + exception.getResponseBodyAsString();
            }
        }

        return explication;
    }
}
