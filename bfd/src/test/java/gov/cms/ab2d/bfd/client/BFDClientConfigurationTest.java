package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.List;

import static gov.cms.ab2d.bfd.client.MockUtils.getRawJson;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpringBootApp.class)
@ContextConfiguration(initializers = {MockUtils.PropertyOverrider.class, BFDClientConfigurationTest.PropertyOverrider.class})
public class BFDClientConfigurationTest {

    @Autowired
    private BFDClientImpl bbc;

    @Autowired
    @Qualifier("bfdHttpClient")
    private HttpClient httpClient;

    @Autowired
    private BfdClientVersions bfdClientVersions;


    private static MockServerClient mockServer;

    private static final String METADATA_PATH = "bb-test-data/meta.json";
    public static final int MOCK_PORT_V1 = MockUtils.randomMockServerPort();

    public static class PropertyOverrider implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String baseUrl = "bfd.url=https://localhost:" + MOCK_PORT_V1;
            addInlinedPropertiesToEnvironment(applicationContext, baseUrl);
        }
    }

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(MOCK_PORT_V1);
        MockUtils.createMockServerExpectation("/v1/fhir/metadata", HttpStatus.SC_OK,
                getRawJson(METADATA_PATH), List
                        .of(), MOCK_PORT_V1);

        // MITM attack private key and cert with same common name as BFD
        ConfigurationProperties.certificateAuthorityPrivateKey(MockUtils.MITM_KEY);
        ConfigurationProperties.certificateAuthorityCertificate(MockUtils.MITM_PEM);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        mockServer.stop();
    }

    @DisplayName("Do not trust self signed certs unless they are part of truststore")
    @Test
    public void doNotTrustSelfSignedCerts() {
        assertNull(bbc.capabilityStatement(STU3));

        // Because this is so important explicitly test the code being run
        Exception exception = assertThrows(FhirClientConnectionException.class, () -> bbc.getCapabilityStatement(STU3));

        assertEquals(SSLHandshakeException.class, exception.getCause().getCause().getClass());
        assertTrue(exception.getCause().getCause()
                .getMessage().contains("unable to find valid certification path to requested target"));
    }

    @DisplayName("Certs with public authority are not trusted unless they are explicitly listed in the truststore")
    @Test
    public void doNotTrustPublicCerts() {
        HttpGet httpget = new HttpGet("https://www.verisign.com/");

        try {
            assertThrows(SSLHandshakeException.class, () -> httpClient.execute(httpget),
                    "Call to verisign should fail with certificate request target exception. Cert is not in truststore.");
        } finally {
            try {
                httpget.releaseConnection();
            } catch (Exception ex) {
                // ignore
            }
        }

    }
}
