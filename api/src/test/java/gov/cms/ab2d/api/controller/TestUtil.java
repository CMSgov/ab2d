package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class TestUtil {

    public static final String TEST_USER = "EileenCFrierson@example.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    private Map<String, String> headerMap;

    // This will expire in 3600 seconds
    public Map<String, String> setupToken() throws IOException, InterruptedException {
        setupUser();

        // Tests run 1 at a time, this may pose an issue if they ever run concurrently
        if(headerMap != null) {
            return headerMap;
        }

        headerMap = new HashMap<>();

        var values = new HashMap<String, String>() {{
            put("grant_type", "password");
            put("username", TEST_USER);
            put("password", "Te$t2019");
            put("scope", "openid");
        }};

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dev-418212.okta.com/oauth2/default/v1/token"))
                .header("accept", "application/json")
                .header("authorization", "Basic MG9hMXB4YXB6ZE9XaUhmOXUzNTc=")
                .header("content-type", "application/x-www-form-urlencoded")
                .POST(buildFormDataFromMap(values))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        headerMap = mapper.readValue(response.body(), Map.class);
        return headerMap;
    }

    // See https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
    private HttpRequest.BodyPublisher buildFormDataFromMap(Map<String, String> data) {
        var builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    private void setupUser() {
        User testUser = userRepository.findByUserName(TEST_USER);
        if(testUser != null) {
            return;
        }

        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent Corp.");
        parent.setHpmsId(456);
        parent.setLegalName("Parent Corp.");

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Test");
        sponsor.setHpmsId(123);
        sponsor.setLegalName("Test");
        sponsor.setParent(parent);
        Sponsor savedSponsor = sponsorRepository.save(sponsor);

        User user = new User();
        user.setEmail(TEST_USER);
        user.setFirstName("Eileen");
        user.setLastName("Frierson");
        user.setUserName(TEST_USER);
        user.setSponsor(savedSponsor);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
