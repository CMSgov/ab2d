package gov.cms.ab2d.e2etest;

import gov.cms.ab2d.common.model.JobStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRunner {

    @Container
    public static DockerComposeContainer container = new DockerComposeContainer(
            new File("../docker-e2e-compose.yml"))
            //.withScaledService("api", 2) // failing now since it's not changing ports
            //.withScaledService("worker", 2)
            .withExposedService("db", 5432)
            .withExposedService("api", 8080);

    private static HttpClient httpClient;

    private static DriverManagerDataSource dataSource;

    private static String AB2D_API_URL = "http://localhost:8080/api/v1/fhir/";

    private static String AB2D_ADMIN_URL = "http://localhost:8080/api/v1/admin/";

    private static final String PATIENT_EXPORT_PATH = "Patient/$export";

    private static String jwtStr = null;

    private static String TEST_USER;

    private static final long DEFAULT_TIMEOUT = 30;

    private static final int THREAD_DELAY = 20000;

    private Integer sponsorId = null;


    @BeforeAll
    public void init() throws IOException, InterruptedException, JSONException {
        container.start();

        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getResourceAsStream("/default-config.yml");
        Map<String, String> yamlMap = yaml.load(inputStream);
        String oktaUrl = yamlMap.get("okta-url");
        TEST_USER = yamlMap.get("username");
        String password = yamlMap.get("password");
        AB2D_API_URL = yamlMap.get("ab2d-api-url");
        AB2D_ADMIN_URL = yamlMap.get("ab2d-admin-url");

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        var jwtRequestParms = new HashMap<>() {{
            put("grant_type", "password");
            put("username", TEST_USER);
            put("password", password);
            put("scope", "openid");
        }};

        HttpRequest jwtRequest = HttpRequest.newBuilder()
                .uri(URI.create(oktaUrl))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("Authorization", "Basic MG9hMXB4YXB6ZE9XaUhmOXUzNTc6WGIxQnpDb1ktS0Y4Z1FhTF9JMmVzVmxsdkpZZ2VzX2d1QTVHZTlpaQ==")
                .POST(buildFormDataFromMap(jwtRequestParms))
                .build();

        HttpResponse<String> jwtResponse = httpClient.send(jwtRequest, HttpResponse.BodyHandlers.ofString());
        String responseJwtString = jwtResponse.body();
        JSONObject responseJsonObject = new JSONObject(responseJwtString);
        jwtStr = responseJsonObject.getString("access_token");

        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/ab2d");
        dataSource.setUsername("ab2d");
        dataSource.setPassword("ab2d");

        uploadOrgStructureReport();
        uploadAttestationReport();
    }

    @BeforeEach
    public void initEach() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM job_output");
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM job");
        }
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

    private HttpResponse<String> uploadOrgStructureReport() throws IOException, InterruptedException {
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
    }

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

    private String getJobUuid() throws SQLException {
        String uuid = null;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT job_uuid FROM job WHERE job_status != 'CANCELLED'");
            while (resultSet.next()) {
                uuid = resultSet.getString("job_uuid");
            }
        }

        return uuid;
    }

    private Set<String> getAllJobUuids() throws SQLException {
        Set<String> uuids = new HashSet<>();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT job_uuid FROM job");
            while (resultSet.next()) {
                uuids.add(resultSet.getString("job_uuid"));
            }
        }

        return uuids;
    }

    private Map<String, Object> getJobWithOutput() throws SQLException {
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

    @Test
    public void runSystemWideExport() throws IOException, InterruptedException, JSONException, SQLException {
        HttpResponse<String> exportResponse = exportRequest();

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        HttpResponse<String> secondExportResponse = exportRequest();
        Assert.assertEquals(429, secondExportResponse.statusCode());

        HttpResponse<String> statusResponse = statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(202, statusResponse.statusCode());
        List<String> retryAfterList = statusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterList.iterator().next(), "5");
        List<String> xProgressList = statusResponse.headers().map().get("x-progress");
        Assert.assertEquals(xProgressList.iterator().next(), "0% complete");

        HttpResponse<String> retryStatusResponse = statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(429, retryStatusResponse.statusCode());
        List<String> retryAfterListRepeat = retryStatusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterListRepeat.iterator().next(), "5");

        String jobUUid = getJobUuid();

        HttpResponse<String> deleteResponse = cancelJobRequest(jobUUid);
        Assert.assertEquals(202, deleteResponse.statusCode());

        HttpResponse<String> exportResponseSecondTry = exportRequest();

        Assert.assertEquals(202, exportResponseSecondTry.statusCode());
        List<String> contentLocationListSecondTry = exportResponseSecondTry.headers().map().get("content-location");

        Thread.sleep(THREAD_DELAY);

        HttpResponse<String> statusResponseAgain = statusRequest(contentLocationListSecondTry.iterator().next());

        Assert.assertEquals(200, statusResponseAgain.statusCode());
        final JSONObject json = new JSONObject(statusResponseAgain.body());

        jobUUid = getJobUuid();

        Boolean requiresAccessToken = json.getBoolean("requiresAccessToken");
        Assert.assertEquals(true, requiresAccessToken);
        String request = json.getString("request");
        Assert.assertEquals(request, exportResponse.request().uri().toString());
        JSONArray errors = json.getJSONArray("error");
        Assert.assertEquals(0, errors.length());

        JSONArray output = json.getJSONArray("output");
        JSONObject outputObject = output.getJSONObject(0);
        String url = outputObject.getString("url");
        Assert.assertEquals(url, AB2D_API_URL + "Job/" + jobUUid + "/file/S0001.ndjson");
        String type = outputObject.getString("type");
        Assert.assertEquals(type, "null");

        HttpResponse<String> downloadResponse = fileDownloadRequest(jobUUid, "S0001.ndjson");
        Assert.assertEquals(200, downloadResponse.statusCode());
        String fileContent = downloadResponse.body();

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

        Map<String, Object> jobData = getJobWithOutput();
        Assert.assertEquals(jobUUid, jobData.get("job_uuid"));
        Assert.assertEquals(JobStatus.SUCCESSFUL.name(), jobData.get("status"));
        Assert.assertEquals("100%", jobData.get("status_message"));
        Assert.assertEquals(100, jobData.get("progress"));
        Assert.assertEquals(null, jobData.get("resource_types"));
        Assert.assertEquals(null, jobData.get("fhir_resource_type"));
        Assert.assertEquals(0, jobData.get("contract_id")); // In JDBC SQL, null comes back as 0
    }

    @Test
    public void runContractNumberExport() throws IOException, InterruptedException, SQLException, JSONException {
        HttpResponse<String> exportResponse = exportByContractRequest("S0001");

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        HttpResponse<String> secondExportResponse = exportByContractRequest("S0001");
        Assert.assertEquals(429, secondExportResponse.statusCode());

        HttpResponse<String> statusResponse = statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(202, statusResponse.statusCode());
        List<String> retryAfterList = statusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterList.iterator().next(), "5");
        List<String> xProgressList = statusResponse.headers().map().get("x-progress");
        Assert.assertEquals(xProgressList.iterator().next(), "0% complete");

        HttpResponse<String> retryStatusResponse = statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(429, retryStatusResponse.statusCode());
        List<String> retryAfterListRepeat = retryStatusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterListRepeat.iterator().next(), "5");

        String jobUUid = getJobUuid();

        HttpResponse<String> deleteResponse = cancelJobRequest(jobUUid);
        Assert.assertEquals(202, deleteResponse.statusCode());

        HttpResponse<String> exportResponseSecondTry = exportByContractRequest("S0001");

        Assert.assertEquals(202, exportResponseSecondTry.statusCode());
        List<String> contentLocationListSecondTry = exportResponseSecondTry.headers().map().get("content-location");

        Thread.sleep(THREAD_DELAY);

        HttpResponse<String> statusResponseAgain = statusRequest(contentLocationListSecondTry.iterator().next());

        Assert.assertEquals(200, statusResponseAgain.statusCode());
        final JSONObject json = new JSONObject(statusResponseAgain.body());

        jobUUid = getJobUuid();

        Boolean requiresAccessToken = json.getBoolean("requiresAccessToken");
        Assert.assertEquals(true, requiresAccessToken);
        String request = json.getString("request");
        Assert.assertEquals(request, exportResponse.request().uri().toString());
        JSONArray errors = json.getJSONArray("error");
        Assert.assertEquals(0, errors.length());

        JSONArray output = json.getJSONArray("output");
        JSONObject outputObject = output.getJSONObject(0);
        String url = outputObject.getString("url");
        Assert.assertEquals(url, AB2D_API_URL + "Job/" + jobUUid + "/file/S0001.ndjson");
        String type = outputObject.getString("type");
        Assert.assertEquals(type, "null");

        HttpResponse<String> downloadResponse = fileDownloadRequest(jobUUid, "S0001.ndjson");
        Assert.assertEquals(200, downloadResponse.statusCode());
        String fileContent = downloadResponse.body();

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

        Map<String, Object> jobData = getJobWithOutput();
        Assert.assertEquals(jobUUid, jobData.get("job_uuid"));
        Assert.assertEquals(JobStatus.SUCCESSFUL.name(), jobData.get("status"));
        Assert.assertEquals("100%", jobData.get("status_message"));
        Assert.assertEquals(100, jobData.get("progress"));
        Assert.assertEquals(null, jobData.get("resource_types"));
        Assert.assertEquals(null, jobData.get("fhir_resource_type"));
        Assert.assertNotNull(jobData.get("contract_id"));
    }

    // Used to test when there are a lot of requests sent to the server in parallel. We will verify that all the requests
    // came back with a successful response
    @Test
    public void stressTest() throws SQLException, InterruptedException, JSONException {
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

        Set<String> jobIds = getAllJobUuids();
        Set<String> statusUrls = new HashSet<>();

        for(HttpResponse<String> httpResponse : exportResponses) {
            Assert.assertEquals(202, httpResponse.statusCode());
            List<String> contentLocationList = httpResponse.headers().map().get("content-location");
            String url = contentLocationList.iterator().next();
            statusUrls.add(url);
            String jobUuid = url.substring(url.indexOf("/Job/") + 5, url.indexOf("/$status"));
            if(!jobIds.contains(jobUuid)) {
                Assert.fail("Job UUID " + jobUuid + " was not found from HTTP Response");
            }
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


    }

    @AfterAll
    public static void cleanup() {
        container.stop();
    }
}
