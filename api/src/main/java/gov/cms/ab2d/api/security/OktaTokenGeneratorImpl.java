package gov.cms.ab2d.api.security;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
@Slf4j
public class OktaTokenGeneratorImpl implements OktaTokenGenerator {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    public String generateToken(String clientID, String clientSecret) throws URISyntaxException, IOException, InterruptedException, JSONException {
        log.info("Received request to generate token");

        String authString = Base64.getEncoder().encodeToString((clientID + ":" + clientSecret).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds"))
                .version(HttpClient.Version.HTTP_2)
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + authString)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            log.error("Did not receive a 200 code from token generation request, received {}", response.statusCode());
            throw new TokenResponseError("Did not receive a 200 code from token generation request, received " + response.statusCode());
        }

        final JSONObject json = new JSONObject(response.body());
        return json.getString("access_token");
    }
}
