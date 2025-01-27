package gov.cms.ab2d.e2etest;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static gov.cms.ab2d.common.util.Constants.*;

@Slf4j
public class APIClient {

    @Getter
    private final HttpClient httpClient;

    private final String ab2dUrl;

    private final static Map<FhirVersion, String> ab2dApiUrl = new HashMap<>();

    private final String authEncoded;

    private final String oktaUrl;

    @Setter
    private long defaultTimeout = 30;

    private String jwtStr;

    public static final String PATIENT_EXPORT_PATH = "Patient/$export";

    public APIClient(String ab2dUrl, String oktaUrl, String oktaClientId, String oktaPassword)
            throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        SSLParameters sslParams = new SSLParameters();
        sslParams.setEndpointIdentificationAlgorithm("");
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(sslContext)
                .sslParameters(sslParams)
                .build();

        this.ab2dUrl = ab2dUrl;
        this.ab2dApiUrl.put(FhirVersion.STU3, buildAB2DAPIUrlV1(ab2dUrl));
        this.ab2dApiUrl.put(FhirVersion.R4, buildAB2DAPIUrlV2(ab2dUrl));

        this.oktaUrl = oktaUrl;
        String clientIdAndSecret = oktaClientId + ":" + oktaPassword;
        authEncoded = Base64.getEncoder().encodeToString(clientIdAndSecret.getBytes());

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

        log.debug("Received JWT response");

        JSONObject responseJsonObject = new JSONObject(responseJwtString);
        jwtStr = responseJsonObject.getString("access_token");
    }

    public HttpResponse<String> exportRequest(Map<Object, Object> params, FhirVersion version) throws IOException, InterruptedException {
        String paramString = buildParameterString(params);
        if(!paramString.equals("")) {
            paramString = "?" + paramString;
        }
        HttpRequest exportRequest = HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl.get(version) + PATIENT_EXPORT_PATH + paramString))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        return httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> exportRequest(String exportType, OffsetDateTime since, FhirVersion version) throws IOException, InterruptedException {
        Map<Object, Object> map  = new HashMap<>();
        map.put("_outputFormat", exportType);
        if (since != null) {
            map.put("_since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return exportRequest(map, version);
    }

    public HttpResponse<String> exportByContractRequest(String contractNumber, String exportType, OffsetDateTime since, FhirVersion version) throws IOException, InterruptedException {
        HttpRequest exportRequest = buildExportByContractRequest(contractNumber, exportType, since, version);
        return httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpRequest buildExportByContractRequest(String contractNumber, String exportType, OffsetDateTime since, FhirVersion version) {
        var jwtRequestParms = new HashMap<>() {{
            put("_outputFormat", exportType);
        }};
        if (since != null) {
            jwtRequestParms.put("_since", since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        String paramString = buildParameterString(jwtRequestParms);
        if(!paramString.equals("")) {
            paramString = "?" + paramString;
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl.get(version) + "Group/" + contractNumber + "/$export" + paramString))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();
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

    public HttpResponse<String> cancelJobRequest(String jobId, FhirVersion version) throws IOException, InterruptedException {
        HttpRequest cancelRequest = HttpRequest.newBuilder()
                .uri(URI.create(ab2dApiUrl.get(version) + "Job/" + jobId + "/$status"))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .DELETE()
                .build();

        return httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<InputStream> fileDownloadRequest(String url) throws IOException, InterruptedException {
        return fileDownloadRequest(url, "gzip, deflate, br");
    }

    // create file download request with 'Accept-Encoding' header - set to null to omit header in request
    public HttpResponse<InputStream> fileDownloadRequest(String url, String acceptEncoding) throws IOException, InterruptedException {
        HttpRequest.Builder fileDownloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .header("Authorization", "Bearer " + jwtStr)
                .GET();

        if (acceptEncoding != null) {
            fileDownloadRequest.header("Accept-Encoding", acceptEncoding);
        }
        return httpClient.send(fileDownloadRequest.build(), HttpResponse.BodyHandlers.ofInputStream());
    }

    public HttpResponse<String> healthCheck() throws IOException, InterruptedException {
        HttpRequest healthCheckRequest = HttpRequest.newBuilder()
                .uri(URI.create(ab2dUrl + HEALTH_ENDPOINT))
                .timeout(Duration.ofSeconds(defaultTimeout))
                .GET()
                .build();

        return httpClient.send(healthCheckRequest, HttpResponse.BodyHandlers.ofString());
    }

    private String buildParameterString(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        return builder.toString();
    }

    private HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        String paramString = buildParameterString(data);

        return HttpRequest.BodyPublishers.ofString(paramString);
    }

    public static String buildAB2DAPIUrl(FhirVersion version) {
        return ab2dApiUrl.get(version);
    }

    public static String buildAB2DAPIUrlV1(String baseUrl) {
        return baseUrl + API_PREFIX_V1 + FHIR_PREFIX + "/";
    }

    public static String buildAB2DAPIUrlV2(String baseUrl) {
        return baseUrl + API_PREFIX_V2 + FHIR_PREFIX + "/";
    }
}

