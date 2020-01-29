package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
/**
 * Credits: most of the code in this class has been adopted from https://github.com/CMSgov/dpc-app
 */
public class BlueButtonClientTest {
    // A random example patient (Jane Doe)
    private static final String TEST_PATIENT_ID = "20140000008325";
    // A patient that only has a single EOB record in bluebutton
    private static final String TEST_SINGLE_EOB_PATIENT_ID = "20140000009893";
    // A patient id that should not exist in bluebutton
    private static final String TEST_NONEXISTENT_PATIENT_ID = "31337";
    private static final String TEST_SLOW_PATIENT_ID = "20010000001111";
    private static final String TEST_NO_RECORD_PATIENT_ID = "20010000001115";

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.xml";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    @Autowired
    private BFDClient bbc;

    private static int mockServerPort = 8083;

    private static ClientAndServer mockServer;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @BeforeClass
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);
        createMockServerExpectation("/v1/fhir/metadata", HttpStatus.OK_200,
                getRawXML(METADATA_PATH), List
                        .of());

        // Ensure timeouts are working.
        createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.OK_200,
                StringUtils.EMPTY,
                Collections.singletonList(Parameter.param("patient", TEST_SLOW_PATIENT_ID)),
                8000
        );

        for (String patientId : TEST_PATIENT_IDS) {
            createMockServerExpectation(
                    "/v1/fhir/Patient/" + patientId,
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".xml"),
                    List.of()
            );

            createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_EOB_PATH_PREFIX + patientId + ".xml"),
                    Collections.singletonList(Parameter.param("patient", patientId))
            );

            createMockServerExpectation(
                    "/v1/fhir/Coverage",
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_COVERAGE_PATH_PREFIX + patientId + ".xml"),
                    Collections
                            .singletonList(Parameter.param("beneficiary", "Patient/" + patientId))
            );
        }

        createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.xml"),
                List.of()
        );

        // Patient that exists, but has no records
        createMockServerExpectation(
                "/v1/fhir/Patient/" + TEST_NO_RECORD_PATIENT_ID,
                HttpStatus.OK_200,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".xml"),
                List.of()
        );
        createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.OK_200,
                getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".xml"),
                Collections.singletonList(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID))
        );

        // Create mocks for pages of the results
        for (String startIndex : List.of("10", "20", "30")) {
            createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.OK_200,
                    getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_" + startIndex + ".xml"),
                    List.of(Parameter.param("patient", TEST_PATIENT_ID),
                            Parameter.param("count", "10"),
                            Parameter.param("startIndex", startIndex))
            );
        }
    }

    @AfterClass
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    public void shouldGetTimedOutOnSlowResponse() {
        thrown.expect(FhirClientConnectionException.class);
        thrown.expectCause(allOf(
                instanceOf(SocketTimeoutException.class),
                hasProperty("message", is("Read timed out"))
        ));

        Bundle response = bbc.requestEOBFromServer(TEST_SLOW_PATIENT_ID);
    }

    @Test
    public void shouldGetEOBFromPatientID() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(32, response.getTotal(), "The demo patient should have exactly 32 EOBs");
    }

    @Test
    public void shouldGetEOBPatientNoRecords() {
        Bundle response = bbc.requestEOBFromServer(TEST_NO_RECORD_PATIENT_ID);
        assertFalse(response.hasEntry());
    }

    @Test
    public void shouldNotHaveNextBundle() {
        Bundle response = bbc.requestEOBFromServer(TEST_SINGLE_EOB_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(1, response.getTotal(), "The demo patient should have exactly 1 EOBs");
        assertNull(response.getLink(Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");
    }

    @Test
    public void shouldHaveNextBundle() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertNotNull(response.getLink(Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");
        Bundle nextResponse = bbc.requestNextBundleFromServer(response);
        assertNotNull(nextResponse, "Should have a next bundle");
        assertEquals(10, nextResponse.getEntry().size());
    }

    @Test
    public void shouldReturnBundleContainingOnlyEOBs() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        response.getEntry().forEach((entry) -> assertEquals(
                entry.getResource().getResourceType(),
                ResourceType.ExplanationOfBenefit,
                "EOB bundles returned by the BlueButton client should only contain EOB objects"
        ));
    }

    @Test
    public void testPersonIds() {
        Bundle response = bbc.requestPatientFromServer("11111");
        assertNotNull(response);
        assertEquals(response.getEntry().size(), 2);
        Patient p1 = (Patient) response.getEntry().get(0).getResource();
        Patient p2 = (Patient) response.getEntry().get(0).getResource();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertTrue(sdf.format(p1.getBirthDate()).equalsIgnoreCase("2014-06-01")
                && sdf.format(p2.getBirthDate()).equalsIgnoreCase("2014-06-01"));
        assertTrue(p1.getName().get(0).getFamily().equalsIgnoreCase("Doe")
                && p2.getName().get(0).getFamily().equalsIgnoreCase("Doe"));
    }

    @Test
    public void shouldHandlePatientsWithOnlyOneEOB() {
        final Bundle response = bbc.requestEOBFromServer(TEST_SINGLE_EOB_PATIENT_ID);
        assertEquals(1, response.getTotal(), "This demo patient should have exactly 1 EOB");
    }

    @Test
    public void shouldThrowExceptionWhenResourceNotFound() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestEOBFromServer(TEST_NONEXISTENT_PATIENT_ID),
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a " +
                        "non-existent patient"
        );
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
    private static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams) {
        var delay = 100;
        createMockServerExpectation(path, respCode, payload, qStringParams, delay);
    }

    private static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams, int delayMs) {
        new MockServerClient("localhost", mockServerPort)
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
                                                "application/fhir+xml;charset=UTF-8")
                                )
                                .withBody(payload)
                                .withDelay(TimeUnit.MILLISECONDS, delayMs)
                );
    }


    private static String getRawXML(String path) throws IOException {
        InputStream sampleData =
                BlueButtonClientTest.class.getClassLoader().getResourceAsStream(path);

        if (sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests",
                    BlueButtonClientTest.class.getName(), path);
        }

        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }
}
