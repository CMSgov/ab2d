package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.controller.RemoteTimeoutException;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.contracts.hmsapi.HPMSAuthResponse;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
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
@Slf4j
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

    private HPMSAuthContext authContext = HPMSAuthContext.emptyContext();

    @PostConstruct
    private void buildFullAuthURI() {
        fullAuthURI = buildFullURI(authURL);
    }

    @Override
    public void buildAuthHeaders(HttpHeaders headers) {
        headers.set("X-API-CONSUMER-ID", hpmsAPIKeyId);
        checkTokenExpiration();

        headers.set(AUTHORIZATION, authContext.getAuthToken());
        // re-injecting cookies using WebClient's cookie handler is even more cumbersome
        headers.set(COOKIE, authContext.getCookies());
    }

    private void checkTokenExpiration() {
        if (authContext.getAuthToken().isBlank() || System.currentTimeMillis() >= authContext.getTokenRefreshAfter()) {
            refreshToken();
        }
    }

    private String timestampToUTC(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneOffset.UTC)
                .toString();
    }

    private synchronized void refreshToken() {
        this.authContext = HPMSAuthContext.emptyContext();
        val newAuthContext = HPMSAuthContext.builder();

        Mono<HPMSAuthResponse> orgInfoMono = webClient
                .post().uri(fullAuthURI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(retrieveAuthRequestPayload())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    newAuthContext.cookies(extractCookies(response.cookies()));
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

            newAuthContext.authToken(authResponse.getAccessToken());
            newAuthContext.tokenExpires(curTime + authResponse.getExpires() * 1000L);
            // Convert seconds to millis at a 80% level to pad refreshing of a token so that we are not in the middle of
            // a significant operation when the token expires.
            newAuthContext.tokenRefreshAfter(curTime + authResponse.getExpires() * 800L);
            this.authContext = newAuthContext.build();
            log.info("Refreshed token; Token expires at {}; Next refresh will be after {}",
                timestampToUTC(authContext.getTokenExpires()),
                timestampToUTC(authContext.getTokenRefreshAfter())
            );
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
        return authContext.getAuthToken();
    }

    long getTokenRefreshAfter() {
        return authContext.getTokenRefreshAfter();
    }

    void clearToken() {
        this.authContext = HPMSAuthContext.emptyContext();
    }

    void cleanup() {
        clearToken();
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

    @AllArgsConstructor
    @Getter
    @Builder
    private static class HPMSAuthContext {
        private final String authToken;
        private final String cookies;
        /** time when token expires */
        private final long tokenExpires;
        /** time when we should request another token */
        private final long tokenRefreshAfter;

        public static HPMSAuthContext emptyContext() {
            return new HPMSAuthContext("", "", 0L, 0L);
        }
    }
}
