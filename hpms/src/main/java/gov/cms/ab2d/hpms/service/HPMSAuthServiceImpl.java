package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.COOKIE;

@Service
public class HPMSAuthServiceImpl extends AbstractHPMSService implements HPMSAuthService {

    @Value("${hpms.base.url}/api/idm/oauth/token")
    private String authURL;

    @Value("${HPMS_AUTH_KEY_ID}")
    private String hpmsAPIKeyId;

    @Value("${HPMS_AUTH_KEY_SECRET}")
    private String hpmsSecret;

    private URI fullAuthURI;

    private volatile String authToken;
    private volatile MultiValueMap<String, ResponseCookie> cookies;

    private volatile long tokenExpires;

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void buildFullAuthURI() {
        fullAuthURI = buildFullURI(authURL);
    }

    @Override
    public void buildAuthHeaders(HttpHeaders headers) {
        headers.set("X-API-CONSUMER-ID", hpmsAPIKeyId);
        headers.set(AUTHORIZATION, retrieveAuthToken());
        // Extracting then re-injecting cookies using  WebClient's cookie handler is even more cumbersome
        headers.set(COOKIE, cookies.entrySet()
                .stream()
                .map(r -> r.getValue()
                        .stream()
                        .map(v -> r.getKey() + "=" + v.getValue()))
                .flatMap(Stream::sorted)
                .collect(Collectors.joining("; ")));
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

        Flux<HPMSAuthResponse> orgInfoFlux = WebClient.create()
                .post().uri(fullAuthURI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(retrieveAuthRequestPayload())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToFlux(r -> {
                    cookies = r.cookies();
                    return r.bodyToFlux(HPMSAuthResponse.class);
                });

        // Cough up blood if we can't get an Auth response in a minute.
        HPMSAuthResponse authResponse = orgInfoFlux.blockFirst(Duration.ofMinutes(1));
        if (authResponse == null) {
            throw new RuntimeException("Failed to procure Auth Token");
        }

        // Convert seconds to millis at a 90% level to pad refreshing of a token so that we are not in the middle of
        // a significant operation when the token expires.
        tokenExpires = currentTimestamp + authResponse.getExpires() * 900;
        authToken = authResponse.getAccessToken();
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
}
