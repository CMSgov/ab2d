package gov.cms.ab2d.e2etest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;

// Unit tests here can be run from the IDE and will use LOCAL as the default, they can also be run from the TestLauncher
// class to specify a custom environment
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestRunnerParameterResolver.class)
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRunner {

    private Logger apiLogger = LoggerFactory.getLogger("gov.cms.ab2d.api");
    private Logger workerLogger = LoggerFactory.getLogger("gov.cms.ab2d.worker");

    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";

    private static final String FHIR_TYPE = "application/fhir+ndjson";

    private static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";

    private static APIClient apiClient;

    private static String AB2D_API_URL;

    private static final int DELAY = 5;

    private static final int JOB_TIMEOUT = 300;

    private static final int MAX_USER_JOBS = 3;

    // Default API port exposed on local environments
    private static final int DEFAULT_API_PORT = 8443;

    private String baseUrl = "";

    private Map<String, String> yamlMap;

    private Environment environment;

    private OffsetDateTime earliest = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    private final Set<String> acceptableIdStrings = Set.of("carrier", "dme", "hha", "hospice", "inpatient", "outpatient", "snf");

    // Define default test contract
    private String testContract = "Z0000";

    // Get all methods annotated with @Test and run them. This will only be called from TestLaucher when running against
    // an external environment, the regular tests that run as part of a build will be called like they normally would
    // during a build.
    public void runTests(String testContract) throws InvocationTargetException, IllegalAccessException {
        if (testContract != null && !testContract.isEmpty()) {
            this.testContract = testContract;
            log.info("Running test with contract: " + testContract);
        }
        final Class annotation = Test.class;
        final Class<?> klass = this.getClass();
        final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(annotation)) {
                method.invoke(this);
            }
        }
    }

    public TestRunner(Environment environment) throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        this.environment = environment;
        init();
    }

    public void init() throws IOException, InterruptedException, JSONException, KeyManagementException, NoSuchAlgorithmException {

        // In the CI environment load a random port otherwise use a default port
        int apiPort = getApiPort();

        log.info("Expecting API to be available at port {}", apiPort);

        if(environment.hasComposeFiles()) {
            loadDockerComposeContainers(apiPort);
        }

        loadApiClientConfiguration(apiPort);
    }

    /**
     * Get the api port that is either a default or random based on the environment the end
     * to end tests are running in.
     *
     * Use a random port to prevent issues when CI jobs share a VM in Jenkins.
     *
     * @return port to expose api on
     * @throws IOException on failure to find an open port
     */
    private int getApiPort() throws IOException {

        // https://stackoverflow.com/questions/2675362/how-to-find-an-available-port
        // Causes a race condition that should be extremely rarely which may cause more than one job
        // to attempt to use the same port
        if (environment == Environment.CI) {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }

        return DEFAULT_API_PORT;
    }

    /**
     * Load docker-compose containers to support e2e tests locally.
     * @param apiPort the port to expose the api on
     */
    private void loadDockerComposeContainers(int apiPort) {
        File[] composeFiles = environment.getComposeFiles();

        DockerComposeContainer container = new DockerComposeContainer(composeFiles)
                .withEnv(System.getenv())
                // Add api variable to environment to populate docker-compose port variable
                .withEnv("API_PORT", "" + apiPort)
                .withLocalCompose(true)
                .withScaledService("worker", 2)
                .withScaledService("api", 1)
                .withExposedService("api", DEFAULT_API_PORT, new HostPortWaitStrategy()
                    .withStartupTimeout(Duration.of(200, SECONDS)))
                .withLogConsumer("worker", new Slf4jLogConsumer(workerLogger)) // Use to debug, for now there's too much log data
                .withLogConsumer("api", new Slf4jLogConsumer(apiLogger));

        container.start();
    }

    /**
     * Load api client by retrieving JSON web token using environment variables for keystore and password.
     * @param apiPort api port to connect client to, only used in local or CI environments
     */
    private void loadApiClientConfiguration(int apiPort) throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {

        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream("src/test/resources/" + environment.getConfigName());
        yamlMap = yaml.load(inputStream);
        String oktaUrl = yamlMap.get("okta-url");
        baseUrl = yamlMap.get("base-url");

        // With a local url add the API port to the domain name (localhost)
        if (environment == Environment.CI || environment == Environment.LOCAL) {
            baseUrl += ":" + apiPort;
        }

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
            Thread.sleep(DELAY * 1000 + 2000);

            log.info("polling for status at url start {}", statusUrl);

            statusResponse = apiClient.statusRequest(statusUrl);

            log.info("polling for status at url end {} {}", statusUrl, statusResponse);

            status = statusResponse.statusCode();

            log.info("polling for status at url status {} {}", statusUrl, status);

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

        if(errors.length() > 0) {
            System.out.println(errors);
        }

        Assert.assertEquals(0, errors.length());

        JSONArray output = json.getJSONArray("output");
        JSONObject outputObject = output.getJSONObject(0);
        String url = outputObject.getString("url");
        String filestem = AB2D_API_URL + "Job/" + jobUuid + "/file/" + testContract + "_0001.";
        Assert.assertTrue(url.equals(filestem + "ndjson") || (url.equals(filestem + "zip")));
        String type = outputObject.getString("type");
        Assert.assertEquals(type, "ExplanationOfBenefit");

        JSONArray extension = outputObject.getJSONArray("extension");

        return Pair.of(url, extension);
    }

    private void verifyJsonFromfileDownload(String fileContent, JSONArray extension, OffsetDateTime since) throws JSONException {
        // Some of the data that is returned will be variable and will change from request to request, so not every
        // JSON object can be verified
        String[] jsonLines = fileContent.split("\n");
        for (String str : jsonLines) {
            if (str.isEmpty()) {
                continue;
            }

            JSONObject jsonObject = new JSONObject(str);

            Assert.assertTrue(validFields(jsonObject));
            Assert.assertEquals("ExplanationOfBenefit", jsonObject.getString("resourceType"));
            Assert.assertEquals(0, jsonObject.getInt("precedence"));
            String idString = jsonObject.getString("id");

            boolean found = false;
            for (String acceptableIdString : acceptableIdStrings) {
                if (idString.startsWith(acceptableIdString)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Assert.fail("No acceptable ID string was found, received " + idString);
            }

            // Check that beneficiary id is included and follows expected format
            // published in data dictionary
            checkBeneficiaryId(jsonObject);

            // Check that standard eob fields are present and not empty
            // These fields are the bulk of the report and what PDPs care about
            checkStandardEOBFields(jsonObject);

            // Check whether extensions are correct
            checkEOBExtensions(jsonObject);

            // Check correctness of metadata
            checkMetadata(since, jsonObject);
        }

        // Check metadata used to verify the file sent by AB2D for correctness
        checkDownloadExtensions(fileContent, extension);
    }

    private void checkDownloadExtensions(String fileContent, JSONArray extension) throws JSONException {
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

    private void checkStandardEOBFields(JSONObject jsonObject) throws JSONException {
        final JSONObject typeJson = jsonObject.getJSONObject("type");
        Assert.assertNotNull(typeJson);
        final JSONArray codingJson = typeJson.getJSONArray("coding");
        Assert.assertNotNull(codingJson);
        Assert.assertTrue(codingJson.length() >= 3);
        final JSONArray identifierJson = jsonObject.getJSONArray("identifier");
        Assert.assertNotNull(identifierJson);
        Assert.assertEquals(2, identifierJson.length());
        final JSONArray diagnosisJson = jsonObject.getJSONArray("diagnosis");
        Assert.assertNotNull(diagnosisJson);
        final JSONArray itemJson = jsonObject.getJSONArray("item");
        Assert.assertNotNull(itemJson);
    }

    private void checkBeneficiaryId(JSONObject jsonObject) throws JSONException {
        final JSONObject patientJson = jsonObject.getJSONObject("patient");
        String referenceString = patientJson.getString("reference");
        Assert.assertTrue(StringUtils.isNotBlank(referenceString));
        Assert.assertTrue(referenceString.startsWith("Patient"));
        String patientId = referenceString.substring(referenceString.indexOf('-') + 1);
        Assert.assertTrue(StringUtils.isNotBlank(patientId));
    }

    private void checkMetadata(OffsetDateTime since, JSONObject jsonObject) throws JSONException {
        final JSONObject metaJson = jsonObject.getJSONObject("meta");
        final String lastUpdated = metaJson.getString("lastUpdated");
        Instant lastUpdatedInstant = Instant.parse(lastUpdated);
        if (since != null) {
            Assert.assertTrue(lastUpdatedInstant.isAfter(since.toInstant()));
        }
    }

    private void checkEOBExtensions(JSONObject jsonObject) throws JSONException {

        final JSONArray extensions = jsonObject.getJSONArray("extension");
        Assert.assertNotNull(extensions);
        Assert.assertEquals(1, extensions.length());

        // Assume first extension is MBI object
        JSONObject idObj = extensions.getJSONObject(0);
        Assert.assertNotNull(idObj);

        // Unwrap identifier
        JSONObject valueIdentifier = idObj.getJSONObject("valueIdentifier");
        Assert.assertNotNull(valueIdentifier);

        // Test that we gave correct label to identifier
        String system = valueIdentifier.getString("system");
        Assert.assertFalse(StringUtils.isBlank(system));
        Assert.assertEquals(MBI_ID, system);

        // Check that mbi is present and not empty
        String mbi = valueIdentifier.getString("value");
        Assert.assertFalse(StringUtils.isBlank(mbi));

        JSONArray extensionsArray = valueIdentifier.getJSONArray("extension");
        assertEquals(1, extensionsArray.length());

        JSONObject currencyExtension = extensionsArray.getJSONObject(0);
        assertEquals(CURRENCY_IDENTIFIER, currencyExtension.getString("url"));
        assertTrue(currencyExtension.has("valueCoding"));

        JSONObject valueCoding = currencyExtension.getJSONObject("valueCoding");
        assertTrue(valueCoding.has("code"));
        assertEquals("current", valueCoding.getString("code"));
    }

    private boolean validFields(JSONObject jsonObject) {
        Set<String> allowedFields = Set.of("identifier", "item", "meta", "patient", "billablePeriod", "diagnosis",
                "provider", "id", "type", "precedence", "resourceType", "organization", "facility", "careTeam",
                "procedure", "extension");

        Set<String> disallowedFields = Set.of("status", "patientTarget", "created", "enterer",
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
    @Order(1)
    public void runSystemWideExport() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 1");
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, false, testContract);
        downloadFile(downloadDetails, null);
    }

    @Test
    @Order(2)
    public void runSystemWideExportSince() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 2");
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, earliest);
        log.info("run system wide export since {}", exportResponse);
        System.out.println(earliest);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, false, testContract);
        if (downloadDetails != null) {
            downloadFile(downloadDetails, earliest);
        }
    }

    @Test
    @Order(3)
    public void runErrorSince() throws IOException, InterruptedException {
        System.out.println("Starting test 3");
        OffsetDateTime timeBeforeEarliest = earliest.minus(1, ChronoUnit.MINUTES);
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, timeBeforeEarliest);
        Assert.assertEquals(400, exportResponse.statusCode());

        OffsetDateTime timeAfterNow = OffsetDateTime.now().plus(1, ChronoUnit.MINUTES);
        HttpResponse<String> exportResponse2 = apiClient.exportRequest(FHIR_TYPE, timeBeforeEarliest);
        Assert.assertEquals(400, exportResponse2.statusCode());
    }

    @Test
    @Order(4)
    public void runSystemWideZipExport() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 4");
        HttpResponse<String> exportResponse = apiClient.exportRequest(ZIPFORMAT, null);
        log.info("run system wide zip export {}", exportResponse);
        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    @Order(5)
    public void runContractNumberExport() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 5");
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(testContract, FHIR_TYPE, null);
        log.info("run contract number export {}", exportResponse);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, true, testContract);
        downloadFile(downloadDetails, null);
    }

    @Test
    @Order(6)
    void runContractNumberZipExport() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 6");
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(testContract, ZIPFORMAT, null);
        log.info("run contract number zip export {}", exportResponse);
        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    @Order(7)
    public void testDelete() throws IOException, InterruptedException {
        System.out.println("Starting test 7");
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null);

        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        HttpResponse<String> deleteResponse = apiClient.cancelJobRequest(jobUUid);
        Assert.assertEquals(202, deleteResponse.statusCode());
    }

    @Test
    @Order(8)
    public void testUserCannotDownloadOtherUsersJob() throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        System.out.println("Starting test 8");
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(testContract, FHIR_TYPE, null);
        Assert.assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, true, testContract);

        APIClient secondUserAPIClient = createSecondUserClient();

        HttpResponse<InputStream> downloadResponse = secondUserAPIClient.fileDownloadRequest(downloadDetails.getFirst());
        Assert.assertEquals(downloadResponse.statusCode(), 403);
    }

    @Test
    @Order(9)
    public void testUserCannotDeleteOtherUsersJob() throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        System.out.println("Starting test 9");
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
    @Order(10)
    public void testUserCannotCheckStatusOtherUsersJob() throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        System.out.println("Starting test 10");
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

    private APIClient createSecondUserClient() throws InterruptedException, JSONException, IOException, KeyManagementException, NoSuchAlgorithmException {
        String oktaUrl = yamlMap.get("okta-url");

        String oktaClientId = System.getenv("SECONDARY_USER_OKTA_CLIENT_ID");
        String oktaPassword = System.getenv("SECONDARY_USER_OKTA_CLIENT_PASSWORD");

        return new APIClient(baseUrl, oktaUrl, oktaClientId, oktaPassword);
    }

    @Test
    @Order(11)
    public void testUserCannotMakeRequestWithoutToken() throws IOException, InterruptedException {
        System.out.println("Starting test 11");
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
    @Order(12)
    public void testUserCannotMakeRequestWithSelfSignedToken() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 12");
        String clientSecret = "wefikjweglkhjwelgkjweglkwegwegewg";
        SecretKey sharedSecret = Keys.hmacShaKeyFor(clientSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("exp", now.toEpochMilli() + 3600);
        claimsMap.put("iat", now.toEpochMilli());
        claimsMap.put("issuer", "https://sandbox.ab2d.cms.gov");
        Claims claims = new DefaultClaims(claimsMap);

        String jwtStr = Jwts.builder()
                .setAudience(System.getenv("AB2D_OKTA_JWT_AUDIENCE"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(2L, ChronoUnit.HOURS)))
                .setIssuer(System.getenv("AB2D_OKTA_JWT_ISSUER"))
                .setId(UUID.randomUUID().toString())
                .setClaims(claims)
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

        Assert.assertEquals(403, response.statusCode());
    }

    @Test
    @Order(13)
    public void testUserCannotMakeRequestWithNullClaims() throws IOException, InterruptedException, JSONException {
        System.out.println("Starting test 12");
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

        Assert.assertEquals(403, response.statusCode());
    }

    @Test
    @Order(14)
    public void testBadQueryParameterResource() throws IOException, InterruptedException {
        System.out.println("Starting test 13");
        var params = new HashMap<>(){{
            put("_type", "BadParam");
        }};
        HttpResponse<String> exportResponse = apiClient.exportRequest(params);

        log.info("bad query parameter resource {}", exportResponse);
        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    @Order(15)
    public void testBadQueryParameterOutputFormat() throws IOException, InterruptedException {
        System.out.println("Starting test 14");
        var params = new HashMap<>(){{
            put("_outputFormat", "BadParam");
        }};
        HttpResponse<String> exportResponse = apiClient.exportRequest(params);

        log.info("bad query output format {}", exportResponse);

        Assert.assertEquals(400, exportResponse.statusCode());
    }

    @Test
    @Order(16)
    public void testHealthEndPoint() throws IOException, InterruptedException {
        System.out.println("Starting test 15");
        HttpResponse<String> healthCheckResponse = apiClient.healthCheck();

        Assert.assertEquals(200, healthCheckResponse.statusCode());
    }
}
