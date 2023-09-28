package gov.cms.ab2d.e2etest;

import gov.cms.ab2d.fhir.FhirVersion;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.security.Keys;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.yaml.snakeyaml.Yaml;


import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.e2etest.APIClient.PATIENT_EXPORT_PATH;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// Unit tests here can be run from the IDE and will use LOCAL as the default, they can also be run from the TestLauncher
// class to specify a custom environment
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestRunnerParameterResolver.class)
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestRunner {

    private static final Logger apiLogger = LoggerFactory.getLogger("gov.cms.ab2d.api");
    private static final Logger workerLogger = LoggerFactory.getLogger("gov.cms.ab2d.worker");

    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";

    private static final String FHIR_TYPE = "application/fhir+ndjson";

    private static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";

    private static APIClient apiClient;

    private static final int DELAY = 5;

    private static final int JOB_TIMEOUT = 300;

    // Default API port exposed on local environments
    private static final int DEFAULT_API_PORT = 8443;

    private String baseUrl = "";

    private Map<String, String> yamlMap;

    private final Environment environment;

    private static final OffsetDateTime earliest = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    private final Set<String> acceptableIdStrings = Set.of("carrier", "dme", "hha", "hospice", "inpatient", "outpatient", "snf");

    // Get all methods annotated with @Test and run them. This will only be called from TestLaucher when running against
    // an external environment, the regular tests that run as part of a build will be called like they normally would
    // during a build.
    public void runTests(String testContract) throws InvocationTargetException, IllegalAccessException {
        if (testContract != null && !testContract.isEmpty()) {
            log.info("Running test with contract: " + testContract);
        }
        final Class<Test> annotation = Test.class;
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

        if (environment.hasComposeFiles()) {
            loadDockerComposeContainers(apiPort);
        }

        loadApiClientConfiguration(apiPort);
    }

    /**
     * Get the api port that is either a default or random based on the environment the end
     * to end tests are running in.
     * <p>
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
     *
     * @param apiPort the port to expose the api on
     */
    private void loadDockerComposeContainers(int apiPort) {
        File[] composeFiles = environment.getComposeFiles();

        DockerComposeContainer container = new DockerComposeContainer(composeFiles)
                .withEnv(System.getenv())
                // Add api variable to environment to populate docker-compose port variable
                .withEnv("API_PORT", "" + apiPort)
                /**
                 * Convert environment back to string that is
                 * understandable by {@link gov.cms.ab2d.eventclient.clients.Ab2dEnvironment#fromName(String)} method
                 */
                .withEnv("AB2D_EXECUTION_ENV", environment.getAb2dEnvironment().getName())
                .withLocalCompose(true)
                .withScaledService("worker", 2)
                .withScaledService("api", 1)
                // Used to debug failures in tests by piping container logs to console
                .withLogConsumer("worker", new Slf4jLogConsumer(workerLogger))
                .withLogConsumer("api", new Slf4jLogConsumer(apiLogger));

        try {
            container.start();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }
    }

    /**
     * Load api client by retrieving JSON web token using environment variables for keystore and password.
     *
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

        String oktaClientId = System.getenv("OKTA_CLIENT_ID");
        String oktaPassword = System.getenv("OKTA_CLIENT_PASSWORD");

        apiClient = new APIClient(baseUrl, oktaUrl, oktaClientId, oktaPassword);

        // add in later
        //uploadOrgStructureReport();
        //uploadAttestationReport();
    }

    private HttpResponse<String> pollForStatusResponse(String statusUrl) throws InterruptedException, IOException {
        HttpResponse<String> statusResponse = null;
        long start = System.currentTimeMillis();
        int status = 0;
        Set<Integer> statusesBetween0And100 = new HashSet<>();
        while (status != 200 && status != 500) {
            Thread.sleep(DELAY * 1000 + 2000);

            log.info("polling for status at url start {}", statusUrl);

            statusResponse = apiClient.statusRequest(statusUrl);

            log.info("polling for status at url end {} {}", statusUrl, statusResponse);

            status = statusResponse.statusCode();

            log.info("polling for status at url status {} {}", statusUrl, status);

            List<String> xProgressList = statusResponse.headers().map().get("x-progress");
            if (xProgressList != null && !xProgressList.isEmpty()) {
                String xProgress = xProgressList.iterator().next();
                int xProgressValue = Integer.parseInt(xProgress.substring(0, xProgress.indexOf('%')));
                if (xProgressValue > 0 && xProgressValue < 100) {
                    statusesBetween0And100.add(xProgressValue);
                }
            }
            if (System.currentTimeMillis() - start > (JOB_TIMEOUT * 1000)) {
                break;
            }
        }

        if (status == 200 || status == 500) {
            return statusResponse;
        } else {
            // Instead of doing Assert.fail do this to make the return status happy
            throw new IllegalStateException("Took too long to poll for status while still receiving a 202 code");
        }
    }

    private Pair<String, JSONArray> verifyJsonFromStatusResponse(HttpResponse<String> statusResponse, String jobUuid,
                                                                 boolean hasContract, String contractNumber, FhirVersion version) throws JSONException {
        final JSONObject json = new JSONObject(statusResponse.body());
        Boolean requiresAccessToken = json.getBoolean("requiresAccessToken");
        assertEquals(true, requiresAccessToken);
        String request = json.getString("request");
        String versionUrl = APIClient.buildAB2DAPIUrl(version);

        String stem = versionUrl + (!hasContract ? "Patient/" : "Group/" + contractNumber + "/") + "$export?_outputFormat=";
        assertTrue(request.startsWith(stem));
        JSONArray errors = json.getJSONArray("error");

        if (errors.length() > 0) {
            log.error(errors.toString());
        }

        assertEquals(0, errors.length());

        JSONArray output = json.getJSONArray("output");
        JSONObject outputObject = output.getJSONObject(0);
        String url = outputObject.getString("url");
        String filestem = versionUrl + "Job/" + jobUuid + "/file/" + contractNumber + "_0001.";
        assertTrue(url.equals(filestem + "ndjson") || (url.equals(filestem + "zip")));
        String type = outputObject.getString("type");
        assertEquals("ExplanationOfBenefit", type);

        JSONArray extension = outputObject.getJSONArray("extension");

        return Pair.of(url, extension);
    }

    private void verifyJsonFromfileDownload(String fileContent, JSONArray extension, OffsetDateTime since, FhirVersion version) throws JSONException {
        // Some of the data that is returned will be variable and will change from request to request, so not every
        // JSON object can be verified
        String[] jsonLines = fileContent.split("\n");
        for (String str : jsonLines) {
            if (str.isEmpty()) {
                continue;
            }

            JSONObject jsonObject = new JSONObject(str);

            assertTrue(validFields(jsonObject));
            assertEquals("ExplanationOfBenefit", jsonObject.getString("resourceType"));
            String status = jsonObject.getString("status");
            assertTrue(List.of("active", "cancelled").contains(status));
            String idString = jsonObject.getString("id");

            boolean found = false;
            for (String acceptableIdString : acceptableIdStrings) {
                if (idString.startsWith(acceptableIdString)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                fail("No acceptable ID string was found, received " + idString);
            }

            // Check that beneficiary id is included and follows expected format
            // published in data dictionary
            checkBeneficiaryId(jsonObject);

            // Check that standard eob fields are present and not empty
            // These fields are the bulk of the report and what PDPs care about
            checkStandardEOBFields(jsonObject);

            // Check whether extensions are correct
            checkEOBExtensions(jsonObject, version);

            // Check correctness of metadata
            checkMetadata(since, jsonObject);
        }

        // Check metadata used to verify the file sent by AB2D for correctness
        checkDownloadExtensions(fileContent, extension);
    }

    private void checkDownloadExtensions(String fileContent, JSONArray extension) throws JSONException {
        JSONObject checkSumObject = extension.getJSONObject(0);
        String checkSumUrl = checkSumObject.getString("url");
        assertEquals("https://ab2d.cms.gov/checksum", checkSumUrl);
        String checkSum = checkSumObject.getString("valueString");
        byte[] sha256ByteArr = DigestUtils.sha256(fileContent);
        String sha256Str = Hex.encodeHexString(sha256ByteArr);
        assertEquals("sha256:" + sha256Str, checkSum);
        assertEquals(71, checkSum.length());

        JSONObject lengthObject = extension.getJSONObject(1);
        String lengthUrl = lengthObject.getString("url");

        assertEquals("https://ab2d.cms.gov/file_length", lengthUrl);
        long length = lengthObject.getLong("valueDecimal");
        assertEquals(length, fileContent.getBytes().length);
    }

    private void checkStandardEOBFields(JSONObject jsonObject) throws JSONException {
        final JSONObject typeJson = jsonObject.getJSONObject("type");
        assertNotNull(typeJson);
        final JSONArray codingJson = typeJson.getJSONArray("coding");
        assertNotNull(codingJson);
        assertTrue(codingJson.length() >= 3);
        final JSONArray identifierJson = jsonObject.getJSONArray("identifier");
        assertNotNull(identifierJson);
        assertEquals(2, identifierJson.length());
        final JSONArray diagnosisJson = jsonObject.getJSONArray("diagnosis");
        assertNotNull(diagnosisJson);
        final JSONArray itemJson = jsonObject.getJSONArray("item");
        assertNotNull(itemJson);
    }

    private void checkBeneficiaryId(JSONObject jsonObject) throws JSONException {
        final JSONObject patientJson = jsonObject.getJSONObject("patient");
        String referenceString = patientJson.getString("reference");
        assertTrue(StringUtils.isNotBlank(referenceString));
        assertTrue(referenceString.startsWith("Patient"));
        String patientId = referenceString.substring(referenceString.indexOf('-') + 1);
        assertTrue(StringUtils.isNotBlank(patientId));
    }

    private void checkMetadata(OffsetDateTime since, JSONObject jsonObject) throws JSONException {
        final JSONObject metaJson = jsonObject.getJSONObject("meta");
        final String lastUpdated = metaJson.getString("lastUpdated");
        Instant lastUpdatedInstant = Instant.parse(lastUpdated);
        if (since != null) {
            assertTrue(lastUpdatedInstant.isAfter(since.toInstant()));
        }
    }

    private void checkEOBExtensions(JSONObject jsonObject, FhirVersion version) throws JSONException {
        switch (version) {
            case STU3 -> checkEOBExtensionsSTU3(jsonObject);
            case R4 -> checkEOBExtensionsR4(jsonObject);
            default -> {
            }
        }
    }

    private void checkEOBExtensionsSTU3(JSONObject jsonObject) throws JSONException {

        final JSONArray extensions = jsonObject.getJSONArray("extension");
        assertNotNull(extensions);
        assertEquals(22, extensions.length());

        // Assume first extension is MBI object
        JSONObject idObj = extensions.getJSONObject(21);
        assertNotNull(idObj);

        // Unwrap identifier
        JSONObject valueIdentifier = idObj.getJSONObject("valueIdentifier");
        assertNotNull(valueIdentifier);

        // Test that we gave correct label to identifier
        String system = valueIdentifier.getString("system");
        assertFalse(StringUtils.isBlank(system));
        assertEquals(MBI_ID, system);

        // Check that mbi is present and not empty
        String mbi = valueIdentifier.getString("value");
        assertFalse(StringUtils.isBlank(mbi));

        JSONArray extensionsArray = valueIdentifier.getJSONArray("extension");
        assertEquals(1, extensionsArray.length());

        JSONObject currencyExtension = extensionsArray.getJSONObject(0);
        assertEquals(CURRENCY_IDENTIFIER, currencyExtension.getString("url"));
        assertTrue(currencyExtension.has("valueCoding"));

        JSONObject valueCoding = currencyExtension.getJSONObject("valueCoding");
        assertTrue(valueCoding.has("code"));
        assertEquals("current", valueCoding.getString("code"));
    }

    private void checkEOBExtensionsR4(JSONObject jsonObject) throws JSONException {
        final JSONArray extensions = jsonObject.getJSONArray("extension");
        assertNotNull(extensions);
        assertTrue(extensions.length() == 1 || extensions.length() == 2);
    }

    private boolean validFields(JSONObject jsonObject) {
        Set<String> allowedFields = Set.of("identifier", "status", "item", "meta", "patient", "billablePeriod", "diagnosis",
                "provider", "id", "type", "precedence", "resourceType", "organization", "facility", "careTeam",
                "procedure", "extension", "supportingInfo", "subType");

        Set<String> disallowedFields = Set.of("patientTarget", "created", "enterer",
                "entererTarget", "insurer", "insurerTarget", "providerTarget", "organizationTarget", "referral",
                "referralTarget", "facilityTarget", "claim", "claimTarget", "claimResponse", "claimResponseTarget",
                "outcome", "disposition", "related", "prescription", "prescriptionTarget", "originalPrescription",
                "originalPrescriptionTarget", "payee", "information", "precedence", "insurance", "accident",
                "employmentImpacted", "hospitalization", "addItem", "totalCost", "unallocDeductable", "totalBenefit",
                "payment", "form", "contained", "processNote", "benefitBalance");

        JSONArray obj = jsonObject.names();
        for (int i = 0; i < obj.length(); i++) {
            try {
                String val = (String) obj.get(i);
                if (!allowedFields.contains(val)) {
                    if (disallowedFields.contains(val)) {
                        log.info("********** API outputted invalid field '" + val + "'");
                    } else {
                        log.info("********** API outputted unknown field '" + val + "'");
                    }
                    return false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void downloadFile(Pair<String, JSONArray> downloadDetails, OffsetDateTime since, FhirVersion version) throws IOException, InterruptedException, JSONException {
        HttpResponse<InputStream> downloadResponse = apiClient.fileDownloadRequest(downloadDetails.getFirst());

        assertEquals(200, downloadResponse.statusCode());
        String contentEncoding = downloadResponse.headers().map().get("content-encoding").iterator().next();
        assertEquals("gzip", contentEncoding);

        String downloadString;
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(downloadResponse.body())) {
            downloadString = IOUtils.toString(gzipInputStream, Charset.defaultCharset());
        }

        verifyJsonFromfileDownload(downloadString, downloadDetails.getSecond(), since, version);
    }

    private Pair<String, JSONArray> performStatusRequests(List<String> contentLocationList, boolean isContract,
                                                          String contractNumber, FhirVersion version) throws JSONException, IOException, InterruptedException {
        HttpResponse<String> statusResponse = apiClient.statusRequest(contentLocationList.iterator().next());

        assertEquals(202, statusResponse.statusCode());
        List<String> retryAfterList = statusResponse.headers().map().get("retry-after");
        assertEquals(retryAfterList.iterator().next(), String.valueOf(DELAY));
        List<String> xProgressList = statusResponse.headers().map().get("x-progress");
        assertTrue(xProgressList.iterator().next().matches("\\d+% complete"));

        HttpResponse<String> retryStatusResponse = apiClient.statusRequest(contentLocationList.iterator().next());

        assertEquals(429, retryStatusResponse.statusCode());
        List<String> retryAfterListRepeat = retryStatusResponse.headers().map().get("retry-after");
        assertEquals(retryAfterListRepeat.iterator().next(), String.valueOf(DELAY));

        HttpResponse<String> statusResponseAgain = pollForStatusResponse(contentLocationList.iterator().next());

        String jobUuid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        if (statusResponseAgain.statusCode() == 500) {
            // No values returned if 500
            return null;
        }
        assertEquals(200, statusResponseAgain.statusCode());

        return verifyJsonFromStatusResponse(statusResponseAgain, jobUuid, isContract, contractNumber, version);
    }

    @ParameterizedTest
    @MethodSource("getVersionAndContract")
    @Order(1)
    void runSystemWideExport(FhirVersion version, String contract) throws IOException, InterruptedException, JSONException {
        System.out.println();
        log.info("Starting test 1 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null, version);
        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, false, contract, version);
        assertNotNull(downloadDetails);
        downloadFile(downloadDetails, null, version);
    }

    @ParameterizedTest
    @MethodSource("getVersionAndContract")
    @Order(2)
    void runSystemWideExportSince(FhirVersion version, String contract) throws IOException, InterruptedException, JSONException {
        System.out.println();
        log.info("Starting test 2 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, earliest, version);
        log.info("run system wide export since {}", exportResponse);
        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, false, contract, version);
        assertNotNull(downloadDetails);
        downloadFile(downloadDetails, earliest, version);
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(3)
    void runErrorSince(FhirVersion version) throws IOException, InterruptedException {
        System.out.println();
        log.info("Starting test 3 - " + version.toString());
        OffsetDateTime timeBeforeEarliest = earliest.minus(1, ChronoUnit.MINUTES);
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, timeBeforeEarliest, version);
        assertEquals(400, exportResponse.statusCode());

        OffsetDateTime timeAfterNow = OffsetDateTime.now().plus(1, ChronoUnit.MINUTES);
        HttpResponse<String> exportResponse2 = apiClient.exportRequest(FHIR_TYPE, timeBeforeEarliest, version);
        assertEquals(400, exportResponse2.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersionAndContract")
    @Order(4)
    void runContractNumberExport(FhirVersion version, String contract) throws IOException, InterruptedException, JSONException {
        System.out.println();
        log.info("Starting test 4 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contract, FHIR_TYPE, null, version);
        log.info("run contract number export {}", exportResponse);
        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, true, contract, version);
        assertNotNull(downloadDetails);
        downloadFile(downloadDetails, null, version);
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(5)
    void testDelete(FhirVersion version) throws IOException, InterruptedException {
        System.out.println();
        log.info("Starting test 5 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null, version);

        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        HttpResponse<String> deleteResponse = apiClient.cancelJobRequest(jobUUid, version);
        assertEquals(202, deleteResponse.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersionAndContract")
    @Order(6)
    void testClientCannotDownloadOtherClientsJob(FhirVersion version, String contract) throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        System.out.println();
        log.info("Starting test 6 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contract, FHIR_TYPE, null, version);
        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        Pair<String, JSONArray> downloadDetails = performStatusRequests(contentLocationList, true, contract, version);

        APIClient secondAPIClient = createSecondClient();

        assertNotNull(downloadDetails);
        HttpResponse<InputStream> downloadResponse = secondAPIClient.fileDownloadRequest(downloadDetails.getFirst());
        assertEquals(403, downloadResponse.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(7)
    void testClientCannotDeleteOtherClientsJob(FhirVersion version) throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        System.out.println();
        log.info("Starting test 7 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null, version);

        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());

        APIClient secondAPIClient = createSecondClient();

        HttpResponse<String> deleteResponse = secondAPIClient.cancelJobRequest(jobUUid, version);
        assertEquals(403, deleteResponse.statusCode());

        // Cleanup
        HttpResponse<String> secondDeleteResponse = apiClient.cancelJobRequest(jobUUid, version);
        assertEquals(202, secondDeleteResponse.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(8)
    void testClientCannotCheckStatusOtherClientsJob(FhirVersion version) throws IOException, InterruptedException, JSONException, NoSuchAlgorithmException, KeyManagementException {
        System.out.println();
        log.info("Starting test 8 - " + version.toString());
        HttpResponse<String> exportResponse = apiClient.exportRequest(FHIR_TYPE, null, version);

        assertEquals(202, exportResponse.statusCode());
        List<String> contentLocationList = exportResponse.headers().map().get("content-location");

        APIClient secondAPIClient = createSecondClient();

        HttpResponse<String> statusResponse = secondAPIClient.statusRequest(contentLocationList.iterator().next());
        assertEquals(403, statusResponse.statusCode());

        // Cleanup
        String jobUUid = JobUtil.getJobUuid(contentLocationList.iterator().next());
        HttpResponse<String> secondDeleteResponse = apiClient.cancelJobRequest(jobUUid, version);
        assertEquals(202, secondDeleteResponse.statusCode());
    }

    private APIClient createSecondClient() throws InterruptedException, JSONException, IOException, KeyManagementException, NoSuchAlgorithmException {
        String oktaUrl = yamlMap.get("okta-url");

        String oktaClientId = System.getenv("SECONDARY_USER_OKTA_CLIENT_ID");
        String oktaPassword = System.getenv("SECONDARY_USER_OKTA_CLIENT_PASSWORD");

        return new APIClient(baseUrl, oktaUrl, oktaClientId, oktaPassword);
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(9)
    void testClientCannotMakeRequestWithoutToken(FhirVersion version) throws IOException, InterruptedException {
        System.out.println();
        log.info("Starting test 9 - " + version.toString());
        HttpRequest exportRequest = HttpRequest.newBuilder()
                .uri(URI.create(APIClient.buildAB2DAPIUrl(version) + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = apiClient.getHttpClient().send(exportRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(10)
    void testClientCannotMakeRequestWithSelfSignedToken(FhirVersion version) throws IOException, InterruptedException, JSONException {
        System.out.println();
        log.info("Starting test 10 - " + version.toString());
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
                .uri(URI.create(APIClient.buildAB2DAPIUrl(version) + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        HttpResponse<String> response = apiClient.getHttpClient().send(exportRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(11)
    void testClientCannotMakeRequestWithNullClaims(FhirVersion version) throws IOException, InterruptedException, JSONException {
        System.out.println();
        log.info("Starting test 11 - " + version.toString());
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
                .uri(URI.create(APIClient.buildAB2DAPIUrl(version) + PATIENT_EXPORT_PATH))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtStr)
                .GET()
                .build();

        HttpResponse<String> response = apiClient.getHttpClient().send(exportRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(12)
    void testBadQueryParameterResource(FhirVersion version) throws IOException, InterruptedException {
        System.out.println();
        log.info("Starting test 12 - " + version.toString());
        var params = new HashMap<>() {{
            put("_type", "BadParam");
        }};
        HttpResponse<String> exportResponse = apiClient.exportRequest(params, version);

        log.info("bad query parameter resource {}", exportResponse);
        assertEquals(400, exportResponse.statusCode());
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    @Order(13)
    void testBadQueryParameterOutputFormat(FhirVersion version) throws IOException, InterruptedException {
        System.out.println();
        log.info("Starting test 13 - " + version.toString());
        var params = new HashMap<>() {{
            put("_outputFormat", "BadParam");
        }};
        HttpResponse<String> exportResponse = apiClient.exportRequest(params, version);

        log.info("bad query output format {}", exportResponse);

        assertEquals(400, exportResponse.statusCode());
    }

    @Test
    @Order(14)
    void testHealthEndPoint() throws IOException, InterruptedException {
        System.out.println();
        log.info("Starting test 14");
        HttpResponse<String> healthCheckResponse = apiClient.healthCheck();

        assertEquals(200, healthCheckResponse.statusCode());
    }

    /**
     * Returns the stream of FHIR version and contract to use for that version
     *
     * @return the stream of arguments
     */
    private Stream<Arguments> getVersionAndContract() {
        // Define default test contract
        String testContractV1 = "Z0000";
        if (v2Enabled()) {
            String testContractV2 = "Z0000";
            return Stream.of(arguments(STU3, testContractV1), arguments(R4, testContractV2));
        } else {
            return Stream.of(arguments(STU3, testContractV1));
        }
    }

    /**
     * Return the different versions of FHIR to test against
     *
     * @return the stream of FHIR versions
     */
    static Stream<Arguments> getVersion() {
        if (v2Enabled()) {
            return Stream.of(arguments(STU3), arguments(R4));
        } else {
            return Stream.of(arguments(STU3));
        }
    }

    private static boolean v2Enabled() {
        String v2Enabled = System.getenv("AB2D_V2_ENABLED");
        return v2Enabled != null && v2Enabled.equalsIgnoreCase("true");
    }
}
