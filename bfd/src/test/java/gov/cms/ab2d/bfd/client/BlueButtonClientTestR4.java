package gov.cms.ab2d.bfd.client;

import org.apache.http.HttpStatus;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;

import static gov.cms.ab2d.bfd.client.MockUtils.getRawJson;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

/**
 * Credits: most of the code in this class has been adopted from https://github.com/CMSgov/dpc-app
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpringBootApp.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = {BlueButtonClientTestR4.PropertyOverrider.class})
public class BlueButtonClientTestR4 {
    // A random example patient (Jane Doe)
    private static final Long TEST_PATIENT_ID = -20140000010000L;
    // A patient that only has a single EOB record in bluebutton

    // Paths to test resources
    private static final String METADATA_PATH = "bb-test-data/r4/meta.json";
    private static final String SAMPLE_EOB_BUNDLE = "bb-test-data/r4/eobbundle.json";
    private static final String SAMPLE_PATIENT_BUNDLE = "bb-test-data/r4/patient.json";

    private static final String CONTRACT = "Z0012";
    public static final int MOCK_PORT_V2 = MockUtils.randomMockServerPort();

    // Leave so code coverage works
    @Autowired
    private BFDClientImpl bbc;

    private static ClientAndServer mockServer;

    public static class PropertyOverrider implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String baseUrl = "bfd.url=http://localhost:" + MOCK_PORT_V2;
            addInlinedPropertiesToEnvironment(applicationContext, baseUrl);
        }
    }

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(MOCK_PORT_V2);
        MockUtils.createMockServerExpectation("/v2/fhir/metadata", HttpStatus.SC_OK,
                getRawJson(METADATA_PATH), List.of(), MOCK_PORT_V2);

        // Ensure timeouts are working.
        MockUtils.createMockServerExpectation(
                "/v2/fhir/ExplanationOfBenefit",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_EOB_BUNDLE),
                List.of(Parameter.param("patient", TEST_PATIENT_ID.toString()),
                        Parameter.param("excludeSAMHSA", "true")),
                MOCK_PORT_V2
        );

        MockUtils.createMockServerExpectation(
                "/v2/fhir/Patient",
                HttpStatus.SC_OK,
                getRawJson(SAMPLE_PATIENT_BUNDLE),
                List.of(Parameter.param("_has:Coverage.extension",
                        "https://bluebutton.cms.gov/resources/variables/ptdcntrct" + 12 + "|" + CONTRACT)),
                MOCK_PORT_V2
        );
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    public void shouldGetEOBFromPatientID() {
        org.hl7.fhir.r4.model.Bundle response = (org.hl7.fhir.r4.model.Bundle) bbc.requestEOBFromServer(R4, TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertEquals(260, response.getTotal(), "The demo patient should have exactly 260 EOBs");
    }

    @Test
    public void shouldHaveNextBundle() {
        org.hl7.fhir.r4.model.Bundle response = (org.hl7.fhir.r4.model.Bundle) bbc.requestEOBFromServer(R4, TEST_PATIENT_ID);

        assertNotNull(response, "The demo patient should have a non-null EOB bundle");
        assertNotNull(response.getLink(org.hl7.fhir.r4.model.Bundle.LINK_NEXT),
                "Should have no next link since all the resources are in the bundle");
    }

    @Test
    public void shouldReturnBundleContainingOnlyEOBs() {
        org.hl7.fhir.r4.model.Bundle response = (org.hl7.fhir.r4.model.Bundle) bbc.requestEOBFromServer(R4, TEST_PATIENT_ID);

        response.getEntry().forEach((entry) -> assertEquals(
                entry.getResource().getResourceType(),
                org.hl7.fhir.r4.model.ResourceType.ExplanationOfBenefit,
                "EOB bundles returned by the BlueButton client should only contain EOB objects"
        ));
    }

    @Test
    public void getCoverageData() {
        org.hl7.fhir.r4.model.Bundle response = (org.hl7.fhir.r4.model.Bundle) bbc.requestPartDEnrolleesFromServer(R4, CONTRACT, 12);
        assertNotNull(response);
        List<Bundle.BundleEntryComponent> entries = response.getEntry();
        assertTrue(entries.size() > 0);
        Patient patient = (Patient) entries.get(0).getResource();
        assertNotNull(patient);
        List<Identifier> identifiers = patient.getIdentifier();
        assertTrue(identifiers.stream()
                .filter(c -> c.getSystem().equalsIgnoreCase("http://hl7.org/fhir/sid/us-mbi"))
                .findFirst().isPresent());
        List<Extension> extensions = patient.getExtension();
        assertTrue(extensions.stream()
                .filter(c -> c.getUrl().equalsIgnoreCase("https://bluebutton.cms.gov/resources/variables/rfrnc_yr"))
                .findFirst().isPresent());
    }

    @Test
    public void shouldGetMetadata() {
        org.hl7.fhir.r4.model.CapabilityStatement capabilityStatement = (org.hl7.fhir.r4.model.CapabilityStatement) bbc.capabilityStatement(R4);

        assertNotNull(capabilityStatement, "There should be a non null capability statement");
        assertEquals("4.0.0", capabilityStatement.getFhirVersion().getDisplay());
        assertEquals(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE, capabilityStatement.getStatus());
    }
}
