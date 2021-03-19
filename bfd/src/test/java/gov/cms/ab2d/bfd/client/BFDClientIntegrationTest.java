package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.FhirVersion;
import org.apache.http.client.HttpClient;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled
class BFDClientIntegrationTest {


    private BFDClient sandboxBfdClient;

    @BeforeEach
    void setup() {

        String keystorePath = System.getenv("AB2D_BFD_KEYSTORE_LOCATION");
        String keystorePassword = System.getenv("AB2D_BFD_KEYSTORE_PASSWORD");
        String stu3Url = "https://prod-sbx.bfd.cms.gov/v1/fhir/";
        String r4Url = "https://prod-sbx.bfd.cms.gov/v2/fhir/";

        BFDClientConfiguration configuration =
                new BFDClientConfiguration(keystorePath, keystorePassword,
                        5000, 5000, 5000, 2, 20, 60000
                );

        KeyStore keyStore = configuration.bfdKeyStore();
        HttpClient httpClient = configuration.bfdHttpClient(keyStore);

        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{});

        BfdClientVersions bfdClientVersions = new BfdClientVersions(stu3Url, r4Url, httpClient);

        BFDSearch search = new BFDSearchImpl(httpClient, environment, bfdClientVersions);
        sandboxBfdClient = new BFDClientImpl(search, bfdClientVersions, 10, 10);
    }

    @DisplayName("Live test pulling patient from BFD sandbox patient endpoint by month only")
    @Test
    void getPatientsByMonth() {

        IBaseBundle bundle = sandboxBfdClient.requestPartDEnrolleesFromServer(FhirVersion.STU3, "Z0001", 1);

        Bundle stu3Bundle = (Bundle) bundle;
        assertEquals(10, stu3Bundle.getEntry().size());
    }

    @DisplayName("Live test pulling patient from BFD sandbox patient endpoint by year and month")
    @Test
    void getPatientsByYearAndMonth() {
        IBaseBundle bundle = sandboxBfdClient.requestPartDEnrolleesFromServer(FhirVersion.STU3, "Z0001", 1, 3);

        Bundle stu3Bundle = (Bundle) bundle;
        assertEquals(10, stu3Bundle.getEntry().size());
    }

}
