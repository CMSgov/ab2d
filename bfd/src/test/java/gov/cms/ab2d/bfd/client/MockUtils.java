package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.FhirVersion;
import org.apache.http.HttpStatus;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MockUtils {

    // A random example patient (Jane Doe)
    static final String TEST_PATIENT_ID = "20140000008325";
    // A patient that only has a single EOB record in bluebutton
    static final String TEST_SINGLE_EOB_PATIENT_ID = "20140000009893";
    // A patient id that should not exist in bluebutton
    static final String TEST_NONEXISTENT_PATIENT_ID = "31337";
    static final String TEST_SLOW_PATIENT_ID = "20010000001111";
    static final String TEST_NO_RECORD_PATIENT_ID = "20010000001115";
    static final String TEST_NO_RECORD_PATIENT_ID_MBI = "20010000001116";

    // Paths to test resources
    static final String METADATA_PATH = "bb-test-data/meta.json";
    static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    static final String [] CONTRACT_MONTHS = {"ptdcntrct01", "ptdcntrct02", "ptdcntrct03", "ptdcntrct04",
            "ptdcntrct05", "ptdcntrct06", "ptdcntrct07", "ptdcntrct08", "ptdcntrct09", "ptdcntrct10",
            "ptdcntrct11", "ptdcntrct12"
    };

    static String getRawJson(String path) throws IOException {
        InputStream sampleData =
                BlueButtonClientTestR4.class.getClassLoader().getResourceAsStream(path);

        if (sampleData == null) {
            throw new IOException("Cannot find sample requests for path " + path);
        }

        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Helper method that configures the mock server to respond to a given GET request
     *
     * @param path          The path segment of the URL that would be received by BlueButton
     * @param respCode      The desired HTTP response code
     * @param payload       The data that the mock server should return in response to this GET
     *                      request
     * @param qStringParams The query string parameters that must be present to generate this
     *                      response
     */
    static void createMockServerExpectation(ClientAndServer mockServer, String path, int respCode, String payload,
                                            List<Parameter> qStringParams, int port) {
        var delay = 100;
        createMockServerExpectation(mockServer, path, respCode, payload, qStringParams, delay, port);
    }

    static void createMockServerExpectation(ClientAndServer mockServer, String path, int respCode, String payload,
                                            List<Parameter> qStringParams, int delayMs, int port) {
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("GET")
                                .withPath(path)
                                .withQueryStringParameters(qStringParams),
                        Times.unlimited()
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                                .withStatusCode(respCode)
                                .withHeader(
                                        new Header("Content-Type",
                                                "application/json;charset=UTF-8")
                                )
                                .withBody(payload)
                                .withDelay(TimeUnit.MILLISECONDS, delayMs)
                );
    }

    static int randomMockServerPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException ioException) {
            throw new RuntimeException("could not find open port");
        }
    }

    static void mockPatientIds(ClientAndServer mockServer, String apiVersion, int port) throws IOException {
        for (String patientId : TEST_PATIENT_IDS) {
            createMockServerExpectation(mockServer,
                    "/" + apiVersion + "/fhir/ExplanationOfBenefit",
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_EOB_PATH_PREFIX + patientId + ".json"),
                    List.of(Parameter.param("patient", patientId),
                            Parameter.param("excludeSAMHSA", "true")),
                    port
            );
        }
    }

}
