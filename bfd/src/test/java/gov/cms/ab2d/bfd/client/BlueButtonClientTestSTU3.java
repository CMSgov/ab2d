package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
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
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.bfd.client.MockUtils.*;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Credits: most of the code in this class has been adopted from https://github.com/CMSgov/dpc-app
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpringBootApp.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = { BlueButtonClientTestSTU3.TestConfig.class })
public class BlueButtonClientTestSTU3 {

    private static final String CONTRACT = "S00001";
    public static final int MOCK_PORT_V1 = MockUtils.randomMockServerPort();

    @Autowired
    private BFDClient bbc;

    private static ClientAndServer mockServer;

    // The test data is in XML format, so change the parse so that it can
    @Profile("test")
    @Configuration
    public static class TestConfig {

        @Autowired
        private HttpClient client;

        @Bean
        @Primary
        public BfdClientVersions clientVersions() {
            return new BfdClientVersions("http://localhost:" + MOCK_PORT_V1 + "/v1/fhir/",
            "http://localhost:" + MOCK_PORT_V1 + "/v1/fhir/", client);
        }
    }

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(MOCK_PORT_V1);
        MockUtils.createMockServerExpectation(mockServer, "/v1/fhir/metadata", HttpStatus.SC_OK,
                getRawJson(METADATA_PATH), List
                        .of(), MOCK_PORT_V1);
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    public void shouldGetTimedOutOnSlowResponse() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        // Ensure timeouts are working.
        MockUtils.createMockServerExpectation(
                mockServer,
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                StringUtils.EMPTY,
                Collections.singletonList(Parameter.param("patient", TEST_SLOW_PATIENT_ID)),
                8000,
                MOCK_PORT_V1
        );

        var exception = Assertions.assertThrows(SocketTimeoutException.class, () -> {
            bbc.requestEOBFromServer(STU3, TEST_SLOW_PATIENT_ID);
        });

        var rootCause = ExceptionUtils.getRootCause(exception);
        assertTrue(rootCause instanceof SocketTimeoutException);
        assertEquals("Read timed out", rootCause.getMessage());

    }

    @Test
    public void shouldGetEOBFromPatientID() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(32, response.getTotal(), "The demo patient should have exactly 32 EOBs");
    }

    @Test
    public void shouldGetEOBFromPatientIDSince() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID, OffsetDateTime.parse(
                "2020-02-13T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME));

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(32, response.getTotal(), "The demo patient should have exactly 32 EOBs");
    }

    @Test
    public void shouldGetEOBPatientNoRecords() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        // Patient that exists, but has no records
        MockUtils.createMockServerExpectation(
                mockServer,
                "/v1/fhir/Patient/" + TEST_NO_RECORD_PATIENT_ID,
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_PATIENT_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".json"),
                List.of(),
                MOCK_PORT_V1
        );
        MockUtils.createMockServerExpectation(
                mockServer,
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID + ".json"),
                List.of(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID),
                        Parameter.param("excludeSAMHSA", "true")),
                MOCK_PORT_V1
        );

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_NO_RECORD_PATIENT_ID);
        assertFalse(response.hasEntry());
    }

    @Test
    public void shouldGetEOBPatientNoRecordsMBI() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        MockUtils.createMockServerExpectation(
                mockServer,
                "/v1/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_EOB_PATH_PREFIX + TEST_NO_RECORD_PATIENT_ID_MBI + ".json"),
                List.of(Parameter.param("patient", TEST_NO_RECORD_PATIENT_ID_MBI),
                        Parameter.param("excludeSAMHSA", "true")),
                MOCK_PORT_V1
        );

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_NO_RECORD_PATIENT_ID_MBI);
        assertFalse(response.hasEntry());
    }

    @Test
    public void shouldNotHaveNextBundle() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_SINGLE_EOB_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(1, response.getTotal(), "The demo patient should have exactly 1 EOBs");
        assertNull(response.getLink(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");
    }

    @Test
    public void shouldHaveNextBundle() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertNotNull(response.getLink(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");

        // Change url to point to random mock server port instead of default port
        response.getLink().forEach(link -> {
            String url = link.getUrl().replace("localhost:8083", "localhost:" + MOCK_PORT_V1);
            link.setUrl(url);
        });

        org.hl7.fhir.dstu3.model.Bundle nextResponse = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestNextBundleFromServer(STU3, response);
        assertNotNull(nextResponse, "Should have a next bundle");
        assertEquals(10, nextResponse.getEntry().size());
    }

    @Test
    public void shouldReturnBundleContainingOnlyEOBs() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_PATIENT_ID);

        response.getEntry().forEach((entry) -> assertEquals(
                entry.getResource().getResourceType(),
                org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit,
                "EOB bundles returned by the BlueButton client should only contain EOB objects"
        ));
    }

    @Test
    public void shouldHandlePatientsWithOnlyOneEOB() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestEOBFromServer(STU3, TEST_SINGLE_EOB_PATIENT_ID);
        assertEquals(1, response.getTotal(), "This demo patient should have exactly 1 EOB");
    }

    @Test
    public void shouldThrowExceptionWhenResourceNotFound() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        assertThrows(
                ResourceNotFoundException.class,
                () -> bbc.requestEOBFromServer(STU3, TEST_NONEXISTENT_PATIENT_ID),
                "BlueButton client should throw exceptions when asked to retrieve EOBs for a " +
                        "non-existent patient"
        );
    }

    @Test
    public void shouldGetPatientBundleFromPartDEnrolleeRequestByMonth() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        for(String month : CONTRACT_MONTHS) {
            MockUtils.createMockServerExpectation(
                    mockServer,
                    "/v1/fhir/Patient",
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.json"),
                    List.of(Parameter.param("_has:Coverage.extension",
                            "https://bluebutton.cms.gov/resources/variables/" + month + "|" + CONTRACT)),
                    MOCK_PORT_V1
            );
        }

        for(int i = 1; i <= 12; i++) {
            org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestPartDEnrolleesFromServer(STU3, CONTRACT, i);

            assertNotNull(response, "There should be a non null patient bundle");
            assertEquals(3, response.getEntry().size(), "The bundle has 2 patients");
        }
    }


    @Test
    void shouldGetPatientBundleFromPartDEnrolleeRequestByMonthAndYear() throws IOException {
        for(String month : CONTRACT_MONTHS) {
            MockUtils.createMockServerExpectation(
                    mockServer,
                    "/v1/fhir/Patient",
                    HttpStatus.SC_OK,
                    getRawJson(SAMPLE_PATIENT_PATH_PREFIX + "/bundle/patientbundle.json"),
                    List.of(Parameter.param("_has:Coverage.extension",
                            "https://bluebutton.cms.gov/resources/variables/" + month + "|" + CONTRACT),
                            Parameter.param("_has:Coverage.rfrncyr",
                                    "https://bluebutton.cms.gov/resources/variables/rfrnc_yr|" + 2020)),
                    MOCK_PORT_V1
            );
        }

        for(int i = 1; i <= 12; i++) {
            org.hl7.fhir.dstu3.model.Bundle response = (org.hl7.fhir.dstu3.model.Bundle) bbc.requestPartDEnrolleesFromServer(STU3, CONTRACT, i, 2020);

            assertNotNull(response, "There should be a non null patient bundle");
            assertEquals(3, response.getEntry().size(), "The bundle has 2 patients");
        }
    }

    @Test
    public void shouldGetMetadata() throws IOException {
        mockPatientIds(mockServer,  "v1", MOCK_PORT_V1);

        org.hl7.fhir.dstu3.model.CapabilityStatement capabilityStatement = (org.hl7.fhir.dstu3.model.CapabilityStatement) bbc.capabilityStatement(STU3);

        assertNotNull(capabilityStatement, "There should be a non null capability statement");
        assertEquals("3.0.1", capabilityStatement.getFhirVersion());
        assertEquals(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE, capabilityStatement.getStatus());
    }
}
