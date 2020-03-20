package gov.cms.ab2d.e2etest;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class BFDClient {

    @Getter
    private final HttpClient httpClient;

    private final String bfdUrl;

    @Setter
    private long defaultTimeout = 30;

    public BFDClient(String bfdUrl) {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.bfdUrl = bfdUrl;
    }

    public HttpResponse<String> getPatientWithHICN() throws IOException, InterruptedException {
        HttpRequest patientsWithHICNRequest = HttpRequest.newBuilder()
                .uri(URI.create(bfdUrl + ""))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .GET()
                .build();

        return httpClient.send(patientsWithHICNRequest, HttpResponse.BodyHandlers.ofString());
    }
}
