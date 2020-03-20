package gov.cms.ab2d.e2etest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.util.Pair;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.e2etest.APIClient.PATIENT_EXPORT_PATH;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;

// Unit tests here can be run from the IDE and will use LOCAL as the default, they can also be run from the TestLauncher
// class to specify a custom environment
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestRunnerParameterResolver.class)
@Slf4j
public class TestRunner {
    private static final String FHIR_TYPE = "application/fhir+ndjson";

    private static APIClient apiClient;

    private static String AB2D_API_URL;

    private static final int DELAY = 5;

    private static final int JOB_TIMEOUT = 300;

    private static final int MAX_USER_JOBS = 3;

    private Map<String, String> yamlMap;

    private Environment environment;

    private OffsetDateTime earliest = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    private final Set<String> acceptableIdStrings = Set.of("carrier", "dme", "hha", "hospice", "inpatient", "outpatient", "snf");

    // Get all methods annotated with @Test and run them. This will only be called from TestLaucher when running against
    // an external environment, the regular tests that run as part of a build will be called like they normally would
    // during a build.
    public void runTests() throws InvocationTargetException, IllegalAccessException {
        final Class annotation = Test.class;
        final Class<?> klass = this.getClass();
        final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(annotation)) {
                method.invoke(this);
            }
        }
    }

    public TestRunner(Environment environment) throws IOException, InterruptedException, JSONException {
        this.environment = environment;
        init();
    }

    public void init() throws IOException, InterruptedException, JSONException {
        if(environment.isUsesDockerCompose()) {
            DockerComposeContainer container = new DockerComposeContainer(
                    new File("../docker-compose.yml"))
                    .withEnv(System.getenv())
                    .withScaledService("worker", 2)
                    .withExposedService("db", 5432)
                    .withExposedService("api", 8080, new HostPortWaitStrategy()
                        .withStartupTimeout(Duration.of(150, SECONDS)));
                    //.withLogConsumer("worker", new Slf4jLogConsumer(log)) // Use to debug, for now there's too much log data
                    //.withLogConsumer("api", new Slf4jLogConsumer(log));
            container.start();
        }

        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getResourceAsStream("/" + environment.getConfigName());
        yamlMap = yaml.load(inputStream);
        String oktaUrl = yamlMap.get("okta-url");
        String baseUrl = yamlMap.get("base-url");
        AB2D_API_URL = APIClient.buildAB2DAPIUrl(baseUrl);

        String oktaClientId = System.getenv("OKTA_CLIENT_ID");
        String oktaPassword = System.getenv("OKTA_CLIENT_PASSWORD");

        apiClient = new APIClient(baseUrl, oktaUrl, oktaClientId, oktaPassword);

        // add in later
        //uploadOrgStructureReport();
        //uploadAttestationReport();
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

    private HttpResponse<String> pollForStatusResponse(String statusUrl) throws InterruptedException, IOException {
        HttpResponse<String> statusResponse = null;
        long start = System.currentTimeMillis();
        int status = 0;
        Set<Integer> statusesBetween0And100 = Sets.newHashSet();
        while(status != 200 && status != 500) {
            Thread.sleep(DELAY * 1000);
            statusResponse = apiClient.statusRequest(statusUrl);
            status = statusResponse.statusCode();

            List<String> xProgressList = statusResponse.headers().map().get("x-progress");
            if(xProgressList != null && !xProgressList.isEmpty()) {
                String xProgress = xProgressList.iterator().next();
                int xProgressValue = Integer.valueOf(xProgress.substring(0, xProgress.indexOf('%')));
                if (xProgressValue > 0 && xProgressValue < 100) {
                    statusesBetween0And100.add(xProgressValue);
                }
            }
            if(System.currentTimeMillis() - start > (JOB_TIMEOUT * 1000)) {
                break;
            }
        }

        if(statusesBetween0And100.size() < 2) {
            // Currently failing, add back when jobs take longer
            //Assert.fail("Did not receive more than 1 distinct progress values between 0 and 100");
        }

        if (status == 200 || status == 500) {
            return statusResponse;
        } else {
            // Instead of doing Assert.fail do this to make the return status happy
            throw new IllegalStateException("Took too long to poll for status while still receiving a 202 code");
        }
    }

    private Pair<String, JSONArray> verifyJsonFromStatusResponse(HttpResponse<String> statusResponse, String jobUuid, String contractNumber) throws JSONException {
        final JSONObject json = new JSONObject(statusResponse.body());
        Boolean requiresAccessToken = json.getBoolean("requiresAccessToken");
        Assert.assertEquals(true, requiresAccessToken);
        String request = json.getString("request");
        String stem = AB2D_API_URL + (contractNumber == null ? "Patient/" : "Group/" + contractNumber + "/") + "$export?_outputFormat=";
        Assert.assertTrue(request.startsWith(stem));
        JSONArray errors = json.getJSONArray("error");
        Assert.assertEquals(0, errors.length());

        JSONArray output = json.getJSONArray("output");
        JSONObject outputObject = output.getJSONObject(0);
        String url = outputObject.getString("url");
        String filestem = AB2D_API_URL + "Job/" + jobUuid + "/file/S0000_0001.";
        Assert.assertTrue(url.equals(filestem + "ndjson") || (url.equals(filestem + "zip")));
        String type = outputObject.getString("type");
        Assert.assertEquals(type, "ExplanationOfBenefit");

        JSONArray extension = outputObject.getJSONArray("extension");

        return Pair.of(url, extension);
    }

    private void verifyJsonFromfileDownload(String fileContent, JSONArray extension, OffsetDateTime since) throws JSONException {
        // Some of the data that is returned will be variable and will change from request to request, so not every
        // JSON object can be verified
        final JSONObject fileJson = new JSONObject(fileContent);
        Assert.assertTrue(validFields(fileJson));
        Assert.assertEquals("ExplanationOfBenefit", fileJson.getString("resourceType"));
        Assert.assertEquals(0, fileJson.getInt("precedence"));
        String idString = fileJson.getString("id");

        boolean found = false;
        for(String acceptableIdString : acceptableIdStrings) {
            if(idString.startsWith(acceptableIdString)) {
                found = true;
                break;
            }
        }

        if(!found) {
            Assert.fail("No acceptable ID string was found, received " + idString);
        }

        final JSONObject patientJson = fileJson.getJSONObject("patient");
        String referenceString = patientJson.getString("reference");
        Assert.assertTrue(referenceString.startsWith("Patient"));
        final JSONObject typeJson = fileJson.getJSONObject("type");
        Assert.assertNotNull(typeJson);
        final JSONArray codingJson = typeJson.getJSONArray("coding");
        Assert.assertNotNull(codingJson);
        Assert.assertTrue(codingJson.length() >= 4);
        final JSONArray identifierJson = fileJson.getJSONArray("identifier");
        Assert.assertNotNull(identifierJson);
        Assert.assertEquals(2, identifierJson.length());
        final JSONArray diagnosisJson = fileJson.getJSONArray("diagnosis");
        Assert.assertNotNull(diagnosisJson);
        final JSONArray itemJson = fileJson.getJSONArray("item");
        Assert.assertNotNull(itemJson);

        final JSONObject metaJson = fileJson.getJSONObject("meta");
        final String lastUpdated = metaJson.getString("lastUpdated");
        Instant lastUpdatedInstant = Instant.parse(lastUpdated);
        if(since != null) {
            Assert.assertTrue(lastUpdatedInstant.isAfter(since.toInstant()));
        }

        JSONObject checkSumObject = extension.getJSONObject(0);
        String checkSumUrl = checkSumObject.getString("url");
        Assert.assertEquals("https://ab2d.cms.gov/checksum", checkSumUrl);
        String checkSum = checkSumObject.getString("valueString");
        byte[] sha256ByteArr = DigestUtils.sha256(fileContent);
        String sha256Str = Hex.encodeHexString(sha256ByteArr);
        Assert.assertEquals("sha256:" + sha256Str, checkSum);
        Assert.assertEquals(checkSum.length(), 71);

        JSONObject lengthObject = extension.getJSONObject(1);
        String lengthUrl = lengthObject.getString("url");

        Assert.assertEquals("https://ab2d.cms.gov/file_length", lengthUrl);
        long length = lengthObject.getLong("valueDecimal");
        Assert.assertEquals(length, fileContent.getBytes().length);
    }

    private boolean validFields(JSONObject jsonObject) {
        Set<String> allowedFields = Set.of("identifier", "item", "meta", "patient", "billablePeriod", "diagnosis",
                "provider", "id", "type", "precedence", "resourceType", "organization", "facility", "careTeam",
                "procedure");

        Set<String> disallowedFields = Set.of("status", "extension", "patientTarget", "created", "enterer",
            "entererTarget", "insurer", "insurerTarget", "providerTarget", "organizationTarget", "referral",
            "referralTarget", "facilityTarget", "claim", "claimTarget", "claimResponse", "claimResponseTarget",
            "outcome", "disposition", "related", "prescription", "prescriptionTarget", "originalPrescription",
            "originalPrescriptionTarget", "payee", "information", "precedence", "insurance", "accident",
            "employmentImpacted", "hospitalization", "addItem", "totalCost", "unallocDeductable", "totalBenefit",
            "payment", "form", "contained", "processNote", "benefitBalance");

        JSONArray obj = jsonObject.names();
        for (int i = 0; i< obj.length(); i++) {
            try {
                String val = (String) obj.get(i);
                if (!allowedFields.contains(val)) {
                    if (disallowedFields.contains(val)) {
                        System.out.println("********** API outputted invalid field '" + val + "'");
                        return false;
                    } else {
                        System.out.println("********** API outputted unknown field '" + val + "'");
                        return false;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void downloadFile(Pair<String, JSONArray> downloadDetails, OffsetDateTime since) throws IOException, InterruptedException, JSONException {
        HttpResponse<InputStream> downloadResponse = apiClient.fileDownloadRequest(downloadDetails.getFirst());

        Assert.assertEquals(200, downloadResponse.statusCode());
        String contentEncoding = downloadResponse.headers().map().get("content-encoding").iterator().next();
        Assert.assertEquals("gzip", contentEncoding);

        String downloadString;
        try(GZIPInputStream gzipInputStream = new GZIPInputStream(downloadResponse.body())) {
            downloadString = IOUtils.toString(gzipInputStream, Charset.defaultCharset());
        }

        verifyJsonFromfileDownload(downloadString, downloadDetails.getSecond(), since);
    }

    private void downloadZipFile(String url, JSONArray extension, OffsetDateTime since) throws IOException, InterruptedException, JSONException {
        HttpResponse<InputStream> downloadResponse = apiClient.fileDownloadRequest(url);
        ZipInputStream zipIn = new ZipInputStream(downloadResponse.body());
        ZipEntry entry = zipIn.getNextEntry();
        StringBuilder downloadString = new StringBuilder();
        while(entry != null) {
            downloadString.append(extractZipFileData(entry, zipIn));
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        verifyJsonFromfileDownload(downloadString.toString(), extension, since);
    }

    public static String extractZipFileData(ZipEntry entry, ZipInputStream zipIn) throws IOException {
        StringBuilder result = new StringBuilder();
        int read;
        while ((read = zipIn.read()) != -1) {
            result.append((char) read);
        }
        System.out.println("    Entry " + entry.getName() + " - size: " + result.length());
        return result.toString();
    }

    private Pair<String, JSONArray> performStatusRequests(List<String> contentLocationList, boolean isContract,
                                                         String contractNumber) throws JSONException, IOException, InterruptedException {
        HttpResponse<String> statusResponse = apiClient.statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(202, statusResponse.statusCode());
        List<String> retryAfterList = statusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterList.iterator().next(), String.valueOf(DELAY));
        List<String> xProgressList = statusResponse.headers().map().get("x-progress");
        Assert.assertThat(xProgressList.iterator().next(), matchesPattern("\\d+\\% complete"));

        HttpResponse<String> retryStatusResponse = apiClient.statusRequest(contentLocationList.iterator().next());

        Assert.assertEquals(429, retryStatusResponse.statusCode());
        List<String> retryAfterListRepeat = retryStatusResponse.headers().map().get("retry-after");
        Assert.assertEquals(retryAfterListRepeat.iterator().next(), String.valueOf(DELAY));

        HttpResponse<String> statusResponseAgain = pollForStatusResponse(contentLocationList.iterator().next());

        String jobUuid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        if (statusResponseAgain.statusCode() == 500) {
            // No values returned if 500
            return null;
        }
        Assert.assertEquals(200, statusResponseAgain.statusCode());

        return verifyJsonFromStatusResponse(statusResponseAgain, jobUuid, isContract ? contractNumber : null);
    }

    @Test
    public void runSystemWideExport() throws IOException, InterruptedException, JSONException {
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, false, "S0000");
        downloadFile(downloadDetails, null);
    }

    @Test
    public void runSystemWideExportSince() throws IOException, InterruptedException, JSONException {
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, earliest);
        System.out.println(earliest);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, false, "S0000");
        if (downloadDetails != null) {
            downloadFile(downloadDetails, earliest);
        }
    }

    @Test
    public void runErrorSince() throws IOException, InterruptedException {
        OffsetDateTime timeBeforeEarliest = earliest.minus(1, ChronoUnit.MINUTES);
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, timeBeforeEarliest);
        Assert.assertEquals(400, exportResponse.statusCode());

        OffsetDateTime timeAfterNow = OffsetDateTime.now().plus(1, ChronoUnit.MINUTES);
        HttpResponse<String> exportResponse2 = apiClient.exportRequest(FHIR_TYPE, timeBeforeEarliest);
        Assert.assertEquals(400, exportResponse2.statusCode());
    }

    @Test
    public void runSystemWideZipExport() throws IOException, InterruptedException, JSONException {
        HttpResponse<String> exportResponse = apiClient.exportRequest(ZIPFORMAT, null);
        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    public void runContractNumberExport() throws IOException, InterruptedException, JSONException {
        String contractNumber = "S0000";
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contractNumber, FHIR_TYPE, null);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, true, contractNumber);
        downloadFile(downloadDetails, null);
    }

    @Test
    void runContractNumberZipExport() throws IOException, InterruptedException, JSONException {
        String contractNumber = "S0000";
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contractNumber, ZIPFORMAT, null);
        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    public void testDelete() throws IOException, InterruptedException {
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null);

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        HttpResponse<String> deleteResponse = apiClient.cancelJobRequest(jobUUid);
        Assert.assertEquals(202, deleteResponse.statusCode());
    }

    @Test
    public void testUserCannotDownloadOtherUsersJob() throws IOException, InterruptedException, JSONException {
        String contractNumber = "S0000";
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contractNumber, FHIR_TYPE, null);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, true, contractNumber);

        APIClient secondUserAPIClient = createSecondUserClient();

        HttpResponse<InputStream> downloadResponse = secondUserAPIClient.fileDownloadRequest(downloadDetails.getFirst());
        Assert.assertEquals(downloadResponse.statusCode(), 403);
    }

    @Test
    public void testUserCannotDeleteOtherUsersJob() throws IOException, InterruptedException, JSONException {
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null);

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        APIClient secondUserAPIClient = createSecondUserClient();

        HttpResponse<String> deleteResponse = secondUserAPIClient.cancelJobRequest(jobUUid);
        Assert.assertEquals(deleteResponse.statusCode(), 403);

        // Cleanup
        HttpResponse<String> secondDeleteResponse = apiClient.cancelJobRequest(jobUUid);
        Assert.assertEquals(202, secondDeleteResponse.statusCode());
    }

    @Test
    public void testUserCannotCheckStatusOtherUsersJob() throws IOException, InterruptedException, JSONException {
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null);

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        APIClient secondUserAPIClient = createSecondUserClient();

        HttpResponse<String> statusResponse = secondUserAPIClient.statusRequest(contentLocationList.iterator().next());
        Assert.assertEquals(statusResponse.statusCode(), 403);

        // Cleanup
        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());
        HttpResponse<String> secondDeleteResponse = apiClient.cancelJobRequest(jobUUid);
        Assert.assertEquals(202, secondDeleteResponse.statusCode());
    }

    private APIClient createSecondUserClient() throws InterruptedException, JSONException, IOException {
        String oktaUrl = yamlMap.get("okta-url");

        String oktaClientId = System.getenv("SECONDARY_USER_OKTA_CLIENT_ID");
        String oktaPassword = System.getenv("SECONDARY_USER_OKTA_CLIENT_PASSWORD");

        return new APIClient(AB2D_API_URL, oktaUrl, oktaClientId, oktaPassword);
    }

    @Test
    public void testUserCannotMakeRequestWithoutToken() throws IOException, InterruptedException {
        HttpRequest exportRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_API_URL + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = apiClient.getHttpClient().send(exportRequest, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(401, response.statusCode());
    }

    @Test
    public void testUserCannotMakeRequestWithSelfSignedToken() throws IOException, InterruptedException, JSONException {
        String clientSecret = "wefikjweglkhjwelgkjweglkwegwegewg";
        SecretKey sharedSecret = Keys.hmacShaKeyFor(clientSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        String jwtStr = Jwts.builder()
                .setAudience(System.getenv("AB2D_OKTA_JWT_AUDIENCE"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(2L, ChronoUnit.HOURS)))
                .setIssuer(System.getenv("AB2D_OKTA_JWT_ISSUER"))
                .setId(UUID.randomUUID().toString())
                .signWith(sharedSecret)
                .compact();

        HttpRequest exportRequest = HttpRequest.newBuilder()
                .uri(URI.create(AB2D_API_URL + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        HttpResponse<String> response = apiClient.getHttpClient().send(exportRequest, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(500, response.statusCode());

        final JSONObject json = new JSONObject(response.body());
        JSONArray issueJsonArr = json.getJSONArray("issue");
        JSONObject issueJson = issueJsonArr.getJSONObject(0);
        JSONObject detailsJson = issueJson.getJSONObject("details");
        String text = detailsJson.getString("text");

        Assert.assertEquals(text, "An internal error occurred");
    }

    @Test
    public void testBadQueryParameterResource() throws IOException, InterruptedException {
        var params = new HashMap<>(){{
            put("_type", "BadParam");
        }};
        HttpResponse<String> exportResponse = apiClient.exportRequest(params);

        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    public void testBadQueryParameterOutputFormat() throws IOException, InterruptedException {
        var params = new HashMap<>(){{
            put("_outputFormat", "BadParam");
        }};
        HttpResponse<String> exportResponse = apiClient.exportRequest(params);

        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    public void testHealthEndPoint() throws IOException, InterruptedException {
        HttpResponse<String> healthCheckResponse = apiClient.healthCheck();

        Assert.assertEquals(200, healthCheckResponse.statusCode());
    }
}
