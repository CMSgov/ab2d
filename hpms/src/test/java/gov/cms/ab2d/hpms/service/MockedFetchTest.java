package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
class MockedFetchTest {
    @Autowired
    private HPMSFetcherImpl fetcher;

    @Autowired
    HPMSAuthServiceImpl authService;

    private final MockWebClient client = new MockWebClient();

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();


    @Mock
    private WebClient mockedWebClient;


    @Test
    void attestation() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.attestationRequest(mockedWebClient, webClientStatic, Set.of(new HPMSAttestation()));
            fetcher.retrieveAttestationInfo(this::attestation, List.of("test"));
        }
    }

    @Test
    void org() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            client.orgRequest(mockedWebClient, webClientStatic, List.of(new HPMSOrganizationInfo()));
            fetcher.retrieveSponsorInfo(this::orgCallback);
        }
    }

    private void orgCallback(List<HPMSOrganizationInfo> hpmsOrganizationInfos) {
        assertTrue(hpmsOrganizationInfos.size() > 0);
    }


    private void attestation(Set<HPMSAttestation> hpmsAttestations) {
        assertTrue(hpmsAttestations.size() > 0);
    }


    //Needed because the HPMSFetcherImpl uses authservice that needs to be cleared
    @AfterEach
    public void shutdown() {
        authService.cleanup();
    }
}
