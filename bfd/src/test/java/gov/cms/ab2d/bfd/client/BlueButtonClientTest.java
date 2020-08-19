package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpringBootApp.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = { BlueButtonClientTest.TestConfig.class })
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
    private static final String TEST_NO_RECORD_PATIENT_ID_MBI = "20010000001116";

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/meta.xml";
    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String[] TEST_PATIENT_IDS = {"20140000008325", "20140000009893"};

    private static final String [] CONTRACT_MONTHS = {"ptdcntrct01", "ptdcntrct02", "ptdcntrct03", "ptdcntrct04",
            "ptdcntrct05", "ptdcntrct06", "ptdcntrct07", "ptdcntrct08", "ptdcntrct09", "ptdcntrct10",
            "ptdcntrct11", "ptdcntrct12"
    };
    private static final String CONTRACT = "S00001";

    @Autowired
    private BFDClient bbc;

    private static int mockServerPort = 8083;

    private static ClientAndServer mockServer;

    @Profile("test")
    @Configuration
    public static class TestConfig {

        @Autowired
        private FhirContext fhirContext;

        @Bean
        @Primary
        public IParser testBeanDefinition() {
            return fhirContext.newXmlParser();
        }
    }


    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);
        createMockServerExpectation("/v1/fhir/metadata", HttpStatus.SC_OK,
                getRawXML(METADATA_PATH), List
                        .of());

        // Ensure timeouts are working.
        createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                StringUtils.EMPTY,
                Collections.singletonList(Parameter.param("patient", TEST_SLOW_PATIENT_ID)),
                8000
        );

        for (String patientId : TEST_PATIENT_IDS) {
            createMockServerExpectation(
                    "/v1/fhir/Patient/" + patientId,
                    HttpStatus.SC_OK,
                    getRawXML(SAMPLE_PATIENT_PATH_PREFIX + patientId + ".xml"),
                    List.of()
            );

            createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.SC_OK,
                    getRawXML(SAMPLE_EOB_PATH_PREFIX + patientId + ".xml"),
                    List.of(Parameter.param("patient", patientId),
                            Parameter.param("excludeSAMHSA", "true"))
            );

            createMockServerExpectation(
                    "/v1/fhir/Coverage",
                    HttpStatus.SC_OK,
                    getRawXML(SAMPLE_COVERAGE_PATH_PREFIX + patientId + ".xml"),
                    Collections
                            .singletonList(Parameter.param("beneficiary", "Patient/" + patientId))
            );
        }

        createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.SC_OK,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.xml"),
                List.of()
        );

        // Patient that exists, but has no records
        createMockServerExpectation(
                "/v1/fhir/Patient/" + TEST_NO_RECORD_PATIENT_ID,
                HttpStatus.SC_OK,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".xml"),
                List.of()
        );
        createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".xml"),
                List.of(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID),
                        Parameter.param("excludeSAMHSA", "true"))
        );

        createMockServerExpectation(
                "/v1/fhir/Patient/" + TEST_NO_RECORD_PATIENT_ID_MBI,
                HttpStatus.SC_OK,
                getRawXML(SAMPLE_PATIENT_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID_MBI + ".xml"),
                List.of()
        );
        createMockServerExpectation(
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID_MBI + ".xml"),
                List.of(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID_MBI),
                        Parameter.param("excludeSAMHSA", "true"))
        );

        // Create mocks for pages of the results
        for (String startIndex : List.of("10", "20", "30")) {
            createMockServerExpectation(
                    "/v1/fhir/ExplanationOfBenefit",
                    HttpStatus.SC_OK,
                    getRawXML(SAMPLE_EOB_PATH_PREFIX + TEST_PATIENT_ID + "_" + startIndex + ".xml"),
                    List.of(Parameter.param("patient", TEST_PATIENT_ID),
                            Parameter.param("count", "10"),
                            Parameter.param("startIndex", startIndex),
                            Parameter.param("excludeSAMHSA", "true"))
            );
        }

        for(String month : CONTRACT_MONTHS) {
            createMockServerExpectation(
                    "/v1/fhir/Patient",
                    HttpStatus.SC_OK,
                    getRawXML(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.xml"),
                    List.of(Parameter.param("_has:Coverage.extension",
                            "https://bluebutton.cms.gov/resources/variables/" + month + "|" + CONTRACT))
            );
        }
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    public void shouldGetTimedOutOnSlowResponse() {
        var exception = Assertions.assertThrows(SocketTimeoutException.class, () -> {
            bbc.requestEOBFromServer(TEST_SLOW_PATIENT_ID);
        });

        var rootCause = ExceptionUtils.getRootCause(exception);
        assertTrue(rootCause instanceof SocketTimeoutException);
        assertThat(rootCause.getMessage(), equalTo("Read timed out"));

    }

    @Test
    public void shouldGetEOBFromPatientID() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(32, response.getTotal(), "The demo patient should have exactly 32 EOBs");
    }

    @Test
    public void shouldGetEOBFromPatientIDSince() {
        Bundle response = bbc.requestEOBFromServer(TEST_PATIENT_ID, OffsetDateTime.parse(
                "2020-02-13T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME));

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(32, response.getTotal(), "The demo patient should have exactly 32 EOBs");
    }

    @Test
    public void shouldGetEOBPatientNoRecords() {
        Bundle response = bbc.requestEOBFromServer(TEST_NO_RECORD_PATIENT_ID);
        assertFalse(response.hasEntry());
    }

    @Test
    public void shouldGetEOBPatientNoRecordsMBI() {
        Bundle response = bbc.requestEOBFromServer(TEST_NO_RECORD_PATIENT_ID_MBI);
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
    public void testPersonIdsHICN() {
        Bundle response = bbc.requestPatientByHICN("11111");
        assertNotNull(response);
        assertEquals(response.getEntry().size(), 3);
        Patient p1 = (Patient) response.getEntry().get(0).getResource();
        Patient p2 = (Patient) response.getEntry().get(0).getResource();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertTrue(sdf.format(p1.getBirthDate()).equalsIgnoreCase("2014-06-01")
                && sdf.format(p2.getBirthDate()).equalsIgnoreCase("2014-06-01"));
        assertTrue(p1.getName().get(0).getFamily().equalsIgnoreCase("Doe")
                && p2.getName().get(0).getFamily().equalsIgnoreCase("Doe"));
    }

    @Test
    public void testPersonIdsMBI() {
        Bundle response = bbc.requestPatientByMBI("11111");
        assertNotNull(response);
        assertEquals(response.getEntry().size(), 3);
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

    @Test
    public void shouldGetPatientBundleFromPartDEnrolleeRequest() {
        for(int i = 1; i <= 12; i++) {
            Bundle response = bbc.requestPartDEnrolleesFromServer(CONTRACT, i);

            assertNotNull(response, "There should be a non null patient bundle");
            assertEquals(3, response.getEntry().size(), "The bundle has 2 patients");
        }
    }

    @Test
    public void shouldGetMetadata() {
        CapabilityStatement capabilityStatement = bbc.capabilityStatement();

        assertNotNull(capabilityStatement, "There should be a non null capability statement");
        assertEquals(capabilityStatement.getFhirVersion(), "3.0.1");
        assertEquals(capabilityStatement.getStatus(), Enumerations.PublicationStatus.ACTIVE);
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
