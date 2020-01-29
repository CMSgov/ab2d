package gov.cms.ab2d.common.httpclient;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class APIClient {

    @Getter
    private final HttpClient httpClient;

    private final String ab2dApiUrl;

    private final String authEncoded;

    private final String oktaUrl;

    @Setter
    private long defaultTimeout = 30;

    private String jwtStr;

    private static final String PATIENT_EXPORT_PATH = "Patient/$export";

    public APIClient(String ab2dApiUrl, String oktaUrl, String oktaClientId, String oktaPassword)
            throws IOException, InterruptedException, JSONException {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.ab2dApiUrl = ab2dApiUrl;
        this.oktaUrl = oktaUrl;
        authEncoded = Base64.getEncoder().encodeToString((oktaClientId + ":" + oktaPassword).getBytes());

        generateToken();
    }

    // Could make public later if client needs to regenerate
    private void generateToken() throws JSONException, IOException, InterruptedException {
        var jwtRequestParms = new HashMap<>() {{
            put("grant_type", "client_credentials");
            put("scope", "clientCreds");
        }};

        HttpRequest jwtRequest = HttpRequest.newBuilder()
                .uri(URI.create(oktaUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + authEncoded)
                .POST(buildFormDataFromMap(jwtRequestParms))
                .build();

        HttpResponse<String> jwtResponse = httpClient.send(jwtRequest, HttpResponse.BodyHandlers.ofString());
        String responseJwtString = jwtResponse.body();

        log.debug("Received JWT response {}", responseJwtString);

        JSONObject responseJsonObject = new JSONObject(responseJwtString);
        jwtStr = responseJsonObject.getString("access_token");
    }

    public HttpResponse<String> exportRequest() throws IOException, InterruptedException {
        HttpRequest exportRequest = HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        return httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> exportByContractRequest(String contractNumber) throws IOException, InterruptedException {
        HttpRequest exportRequest = buildExportByContractRequest(contractNumber);

        return httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpRequest buildExportByContractRequest(String contractNumber) {
        return HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl + "Group/" + contractNumber + "/$export"))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();
    }

    public CompletableFuture<HttpResponse<String>> exportByContractRequestAsync(String contractNumber) {
        HttpRequest exportRequest = buildExportByContractRequest(contractNumber);

        return httpClient.sendAsync(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> statusRequest(String url) throws IOException, InterruptedException {
        HttpRequest statusRequest = buildStatusRequest(url);

        return httpClient.send(statusRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpRequest buildStatusRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();
    }

    public CompletableFuture<HttpResponse<String>> statusRequestAsync(String url) {
        HttpRequest statusRequest = buildStatusRequest(url);

        return httpClient.sendAsync(statusRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> cancelJobRequest(String jobId) throws IOException, InterruptedException {
        HttpRequest cancelRequest = HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl + "Job/" + jobId + "/$status"))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .DELETE()
                .build();

        return httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> fileDownloadRequest(String jobId, String fileName) throws IOException, InterruptedException {
        HttpRequest fileDownloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl + "Job/" + jobId + "/file/" + fileName))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        return httpClient.send(fileDownloadRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}
