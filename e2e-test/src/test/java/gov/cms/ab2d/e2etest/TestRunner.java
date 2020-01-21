package gov.cms.ab2d.e2etest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.matchesPattern;

// Unit tests here can be run from the IDE and will use LOCAL as the default, they can also be run from the TestLauncher
// class to specify a custom environment
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestRunnerParameterResolver.class)
public class TestRunner {

    private static HttpClient httpClient;

    private static String AB2D_API_URL;

    private static String AB2D_ADMIN_URL;

    private static final String PATIENT_EXPORT_PATH = "Patient/$export";

    private static String jwtStr = null;

    private static final long DEFAULT_TIMEOUT = 30;

    private static final int DELAY = 30;

    private static final int JOB_TIMEOUT = 300;

    private Environment environment;

    public void runTests() throws InterruptedException, JSONException, IOException {
        runSystemWideExport();
        runContractNumberExport();
        testDelete();
    }

    public TestRunner(Environment environment) throws IOException, InterruptedException, JSONException {
        this.environment = environment;
        init();
    }

    public void init() throws IOException, InterruptedException, JSONException {
        if(environment.isUsesDockerCompose()) {
            DockerComposeContainer container = new DockerComposeContainer(
                    new File("../docker-compose.yml"))
                    //.withScaledService("api", 2) // failing now since it's not changing ports
                    .withScaledService("worker", 2)
                    .withExposedService("db", 5432)
                    .withExposedService("api", 8080);
            container.start();
        }

        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getResourceAsStream("/" + environment.getConfigName());
        Map<String, String> yamlMap = yaml.load(inputStream);
        String oktaUrl = yamlMap.get("okta-url");

        AB2D_API_URL = yamlMap.get("ab2d-api-url");
        AB2D_ADMIN_URL = yamlMap.get("ab2d-admin-url");

        var jwtRequestParms = new HashMap<>() {{
            put("grant_type", "client_credentials");
            put("scope", "clientCreds");
        }};

        String oktaClientId = System.getenv(environment.name() + "_OKTA_CLIENT_ID");
        String oktaPassword = System.getenv(environment.name() + "_OKTA_CLIENT_PASSWORD");

        String authEncoded = Base64.getEncoder().encodeToString((oktaClientId + ":" + oktaPassword).getBytes());

        HttpRequest jwtRequest = HttpRequest.newBuilder()
                .uri(URI.create(oktaUrl))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + authEncoded)
                .POST(buildFormDataFromMap(jwtRequestParms))
                .build();

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> jwtResponse = httpClient.send(jwtRequest, HttpResponse.BodyHandlers.ofString());
        String responseJwtString = jwtResponse.body();
        JSONObject responseJsonObject = new JSONObject(responseJwtString);
        jwtStr = responseJsonObject.getString("access_token");

        // add in later
        //uploadOrgStructureReport();
        //uploadAttestationReport();
    }

    private String getJobUuid(String url) {
        return url.substring(url.indexOf("/Job/") + 5, url.indexOf("/$status"));
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

    /*private HttpResponse<String> uploadOrgStructureReport() throws IOException, InterruptedException {
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_ADMIN_URL + "uploadOrgStructureReport"))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                    return getClass().getResourceAsStream("/parent_org_and_legal_entity_20191031_111812.xls");
                }))
                .build();

        return httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> uploadAttestationReport() throws IOException, InterruptedException {
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_ADMIN_URL + "uploadAttestationReport"))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                    return getClass().getResourceAsStream("/Attestation_Report_Sample.xlsx");
                }))
                .build();

        return httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
    }*/

    private HttpResponse<String> exportRequest() throws IOException, InterruptedException {
        HttpRequest exportRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_API_URL + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        return httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> exportByContractRequest(String contractNumber) throws IOException, InterruptedException {
        HttpRequest exportRequest = buildExportByContractRequest(contractNumber);

        return httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildExportByContractRequest(String contractNumber) {
        return HttpRequest.newBuilder()
                .uri(URI.create(AB2D_API_URL + "Group/" + contractNumber + "/$export"))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();
    }

    private CompletableFuture<HttpResponse<String>> exportByContractRequestAsync(String contractNumber) {
        HttpRequest exportRequest = buildExportByContractRequest(contractNumber);

        return httpClient.sendAsync(exportRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> statusRequest(String url) throws IOException, InterruptedException {
        HttpRequest statusRequest = buildStatusRequest(url);

        return httpClient.send(statusRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildStatusRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();
    }

    private CompletableFuture<HttpResponse<String>> statusRequestAsync(String url) {
        HttpRequest statusRequest = buildStatusRequest(url);

        return httpClient.sendAsync(statusRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> cancelJobRequest(String jobId) throws IOException, InterruptedException {
        HttpRequest cancelRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_API_URL + "Job/" + jobId + "/$status"))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .DELETE()
                .build();

        return httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> fileDownloadRequest(String jobId, String fileName) throws IOException, InterruptedException {
        HttpRequest fileDownloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_API_URL + "Job/" + jobId + "/file/" + fileName))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        return httpClient.send(fileDownloadRequest, HttpResponse.BodyHandlers.ofString());
    }

    /*private Map<String, Object> getJobWithOutput() throws SQLException {
        Map<String, Object> jobData = new HashMap<>();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT j.job_uuid, j.status, " +
                    "j.status_message, j.resource_types, j.progress, j.contract_id, jo.fhir_resource_type FROM job j, job_output jo where j.id = jo.job_id and jo.error = false");
            while (resultSet.next()) {
                jobData.put("job_uuid", resultSet.getString("job_uuid"));
                jobData.put("status", resultSet.getString("status"));
                jobData.put("status_message", resultSet.getString("status_message"));
                jobData.put("progress", resultSet.getInt("progress"));
                jobData.put("resource_types", resultSet.getString("resource_types"));
                jobData.put("fhir_resource_type", resultSet.getString("fhir_resource_type"));
                jobData.put("contract_id", resultSet.getInt("contract_id"));
            }
        }

        return jobData;
    }

    private void createContract(String contractNumber) throws SQLException {
        if(sponsorId == null) {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT id FROM sponsor WHERE hpms_id = 999");
                resultSet.next();
                sponsorId = resultSet.getInt("id");
            }
        }

        OffsetDateTime attestationDateTime = OffsetDateTime.now();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO contract(contract_number, contract_name, " +
                    "sponsor_id, attested_on) VALUES('" + contractNumber + "', '" + contractNumber + "', " + sponsorId + ", '" +
                    attestationDateTime + "')");
        }
    }

    private void createContract(String contractNumber) {

    }*/

    private HttpResponse<String> pollForStatusResponse(String statusUrl) throws InterruptedException, IOException {
        HttpResponse<String> statusResponse = null;
        long start = System.currentTimeMillis();
        int status = 0;
        while(status != 200) {
            Thread.sleep(DELAY * 1000);
            statusResponse = statusRequest(statusUrl);
            status = statusResponse.statusCode();

            if(System.currentTimeMillis() - start > (JOB_TIMEOUT * 1000)) {
                break;
            }
        }

        if(status == 200) {
            return statusResponse;
        } else {
            // Instead of doing Assert.fail do this to make the return status happy
            throw new IllegalStateException("Took too long to poll for status while still receiving a 202 code");
        }
    }

    private void verifyJsonFromStatusResponse(HttpResponse<String> statusResponse, String jobUuid, String contractNumber) throws JSONException {
        final JSONObject json = new JSONObject(statusResponse.body());
        Boolean requiresAccessToken = json.getBoolean("requiresAccessToken");
        Assert.assertEquals(true, requiresAccessToken);
        String request = json.getString("request");
        Assert.assertEquals(request, AB2D_API_URL + (contractNumber == null ? "Patient/$export" : "Group/" + contractNumber +
                "/$export"));
        JSONArray errors = json.getJSONArray("error");
        Assert.assertEquals(0, errors.length());

        JSONArray output = json.getJSONArray("output");
        JSONObject outputObject = output.getJSONObject(0);
        String url = outputObject.getString("url");
        Assert.assertEquals(url, AB2D_API_URL + "Job/" + jobUuid + "/file/S0000.ndjson");
        String type = outputObject.getString("type");
        Assert.assertEquals(type, "ExplanationOfBenefit");
    }

    private void verifyJsonFromfileDownload(String fileContent) throws JSONException {
        // Some of the data that is returned will be variable and will change from request to request, so not every
        // JSON object can be verified
        final JSONObject fileJson = new JSONObject(fileContent);
        Assert.assertEquals(8, fileJson.length());
        Assert.assertEquals("ExplanationOfBenefit", fileJson.getString("resourceType"));
        Assert.assertEquals(0, fileJson.getInt("precedence"));
        String carrierString = fileJson.getString("id");
        Assert.assertTrue(carrierString.startsWith("carrier"));
        final JSONObject patientJson = fileJson.getJSONObject("patient");
        String referenceString = patientJson.getString("reference");
        Assert.assertTrue(referenceString.startsWith("Patient"));
        final JSONObject typeJson = fileJson.getJSONObject("type");
        Assert.assertNotNull(typeJson);
        final JSONArray codingJson = typeJson.getJSONArray("coding");
        Assert.assertNotNull(codingJson);
        Assert.assertEquals(4, codingJson.length());
        final JSONArray identifierJson = fileJson.getJSONArray("identifier");
        Assert.assertNotNull(identifierJson);
        Assert.assertEquals(2, identifierJson.length());
        final JSONArray diagnosisJson = fileJson.getJSONArray("diagnosis");
        Assert.assertNotNull(diagnosisJson);
        final JSONArray itemJson = fileJson.getJSONArray("item");
        Assert.assertNotNull(itemJson);
    }

    @Test
    public void runSystemWideExport() throws IOException, InterruptedException, JSONException {
        HttpResponse<String> exportResponse = exportRequest();

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        HttpResponse<String> secondExportResponse = exportRequest();
        Assert.assertEquals(429, secondExportResponse.statusCode());

        performStatusRequestsAndVerifyDownloads(contentLocationList, false, "S0000");
    }

    private void performStatusRequestsAndVerifyDownloads(List<String> contentLocationList, boolean isContract,
                                                         String contractNumber) throws JSONException, IOException, InterruptedException {
        HttpResponse<String> statusResponse = statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(202, statusResponse.statusCode());
        List<String> retryAfterList = statusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterList.iterator().next(), String.valueOf(DELAY));
        List<String> xProgressList = statusResponse.headers().map().get("x-progress");
        Assert.assertThat(xProgressList.iterator().next(), matchesPattern("\\d+\\% complete"));

        HttpResponse<String> retryStatusResponse = statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(429, retryStatusResponse.statusCode());
        List<String> retryAfterListRepeat = retryStatusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterListRepeat.iterator().next(), String.valueOf(DELAY));

        HttpResponse<String> statusResponseAgain = pollForStatusResponse(contentLocationList.iterator().next());

        String jobUuid = getJobUuid(contentLocationList.iterator().next());

        Assert.assertEquals(200, statusResponseAgain.statusCode());

        verifyJsonFromStatusResponse(statusResponseAgain, jobUuid, isContract ? contractNumber : null);

        HttpResponse<String> downloadResponse = fileDownloadRequest(jobUuid, contractNumber + ".ndjson");
        Assert.assertEquals(200, downloadResponse.statusCode());
        String fileContent = downloadResponse.body();

        verifyJsonFromfileDownload(fileContent);
    }

    @Test
    public void runContractNumberExport() throws IOException, InterruptedException, JSONException {
        String contractNumber = "S0000";
        HttpResponse<String> exportResponse = exportByContractRequest(contractNumber);

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        HttpResponse<String> secondExportResponse = exportByContractRequest(contractNumber);
        Assert.assertEquals(429, secondExportResponse.statusCode());

        performStatusRequestsAndVerifyDownloads(contentLocationList, true, contractNumber);
    }

    @Test
    public void testDelete() throws IOException, InterruptedException {
        HttpResponse<String> exportResponse = exportRequest();

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        String jobUUid = getJobUuid(contentLocationList.iterator().next());

        HttpResponse<String> deleteResponse = cancelJobRequest(jobUUid);
        Assert.assertEquals(202, deleteResponse.statusCode());
    }

    // Used to test when there are a lot of requests sent to the server in parallel. We will verify that all the requests
    // came back with a successful response. Could benchmark to see how it trends over time
    /*@Test
    public void stressTest() throws InterruptedException, JSONException {
        final int threshold = 100;

        List<String> contracts = new ArrayList<>();
        for(int i = 2; i < threshold; i++) {
            String contractNumber = "S000" + i;
            createContract(contractNumber);
            contracts.add(contractNumber);
        }

        List<HttpResponse<String>> exportResponses = new ArrayList<>();
        CountDownLatch exportCountDownLatch = new CountDownLatch(contracts.size());
        // Execute HTTP Requests async and gather responses, continue when all are complete, by default these will
        // execute on the ForkJoin common pool
        for(String contract : contracts) {
            exportByContractRequestAsync(contract).thenAcceptAsync(stringHttpResponse -> {
                System.out.println("Executing on thread: " + Thread.currentThread().getName());
                exportResponses.add(stringHttpResponse);
                exportCountDownLatch.countDown();
            });
        }

        exportCountDownLatch.await();

        //Set<String> jobIds = getAllJobUuids();
        Set<String> statusUrls = new HashSet<>();

        for(HttpResponse<String> httpResponse : exportResponses) {
            Assert.assertEquals(202, httpResponse.statusCode());
            List<String> contentLocationList = httpResponse.headers().map().get("content-location");
            String url = contentLocationList.iterator().next();
            statusUrls.add(url);
            String jobUuid = getJobUuid(url);
            //if(!jobIds.contains(jobUuid)) {
                //Assert.fail("Job UUID " + jobUuid + " was not found from HTTP Response");
            //}
        }

        CountDownLatch statusCountdownLatch = new CountDownLatch(contracts.size());

        List<HttpResponse<String>> statusResponses = new ArrayList<>();
        for(String statusUrl : statusUrls) {
            statusRequestAsync(statusUrl).thenAcceptAsync(stringHttpResponse -> {
                statusResponses.add(stringHttpResponse);
                statusCountdownLatch.countDown();
            });
        }

        statusCountdownLatch.await();

        Set<String> downloadUrls = new HashSet<>(contracts.size());

        for(HttpResponse<String> statusResponse : statusResponses) {
            Assert.assertEquals(200, statusResponse.statusCode());

            final JSONObject json = new JSONObject(statusResponse.body());
            JSONArray output = json.getJSONArray("output");
            JSONObject outputObject = output.getJSONObject(0);
            String url = outputObject.getString("url");
            downloadUrls.add(url);
        }
    }*/
}
