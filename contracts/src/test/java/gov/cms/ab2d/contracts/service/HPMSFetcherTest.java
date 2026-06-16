package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.hmsapi.HPMSAttestation;
import gov.cms.ab2d.contracts.hmsapi.HPMSAuthResponse;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;
import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "/application.properties")
@Testcontainers
class HPMSFetcherTest {

    final int NUM_CONTRACTS = 6;

    @Autowired
    private HPMSFetcher fetcher;

    @Autowired
    private HPMSAuthServiceImpl hpmsAuthService;

    @MockitoBean
    private WebClient mockedWebClient;

    @Autowired
    MockWebClient mockWebClient;

    private volatile List<HPMSOrganizationInfo> orgs;

    private volatile Set<HPMSAttestation> attestations;

    private CountDownLatch lock = new CountDownLatch(1);

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    void retrieveSponsorInfo() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            setupMocks(webClientStatic);
            retrieveTop6Contracts();
        }
    }

    @Test
    void retrieveSponsorInfoNull() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            setupMocks(webClientStatic);
            Assertions.assertDoesNotThrow(() -> fetcher.retrieveSponsorInfo());
        }
    }

    @Test
    void retrieveAttestationInfo() {
        try (MockedStatic<WebClient> webClientStatic = Mockito.mockStatic(WebClient.class)) {
            setupMocks(webClientStatic);
            List<String> top6Contracts = retrieveTop6Contracts();
            lock = new CountDownLatch(1);
            attestations = fetcher.retrieveAttestationInfo(top6Contracts);
            assertNotNull(attestations);
            assertFalse(attestations.isEmpty());
            assertTrue(attestations.size() <= NUM_CONTRACTS);
        }
    }

    private void setupMocks(MockedStatic<WebClient> webClientStatic) {
        HPMSAuthResponse authResponse = new HPMSAuthResponse();
        authResponse.setAccessToken("TEST_TOKEN");
        authResponse.setExpires(3600);
        mockWebClient.authRequestError(mockedWebClient, webClientStatic, OK, authResponse);

        Object[] orgArray = new Object[]{
            new HPMSOrganizationInfo("ABC Org", 5, "S1234", "Contract ABC", "ABC Marketing"),
            new HPMSOrganizationInfo("NBC Org", 6, "S2341", "Contract NBC", "NBC Marketing"),
            new HPMSOrganizationInfo("CBS Org", 7, "S3412", "Contract CBS", "CBS Marketing"),
            new HPMSOrganizationInfo("TNT Org", 8, "S4123", "Contract TNT", "TNT Marketing"),
            new HPMSOrganizationInfo("ESPN Org", 9, "S5234", "Contract ESPN", "ESPN Marketing"),
            new HPMSOrganizationInfo("FOX Org", 10, "S6345", "Contract FOX", "FOX Marketing"),
        };
        when(mockWebClient.responseSpec.bodyToMono(Object[].class)).thenReturn(Mono.just(orgArray));

        Set<HPMSAttestation> attestationSet = Set.of(
            new HPMSAttestation("S1234", true, "2020-01-31 14:57:34", null),
            new HPMSAttestation("S2341", true, "2020-02-13 14:57:34", null),
            new HPMSAttestation("S3412", true, "2020-03-24 14:57:34", null),
            new HPMSAttestation("S4123", true, "2020-04-15 14:57:34", null),
            new HPMSAttestation("S5234", true, "2020-05-01 14:57:34", null),
            new HPMSAttestation("S6345", true, "2020-06-10 14:57:34", null)
        );
        when(mockWebClient.responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(attestationSet));
    }

    List<String> retrieveTop6Contracts() {
        orgs = fetcher.retrieveSponsorInfo();
        assertNotNull(orgs);
        assertFalse(orgs.isEmpty());

        List<String> top6Contracts = extractTopContractIDs(NUM_CONTRACTS);
        assertNotNull(top6Contracts);
        assertFalse(top6Contracts.isEmpty());
        return top6Contracts;
    }

    @SuppressWarnings("SameParameterValue")
    private List<String> extractTopContractIDs(int limit) {
        List<String> retList = new ArrayList<>(limit);
        final int sponsorSize = orgs.size();
        for (int idx = 0; idx < limit && idx < sponsorSize; idx++) {
            retList.add(orgs.get(idx).getContractId());
        }
        return retList;
    }

    private void processOrgInfo(List<HPMSOrganizationInfo> orgInfo) {
        orgs = orgInfo;
        lock.countDown();
    }

    private void processAttestations(Set<HPMSAttestation> hpmsAttestations) {
        attestations = hpmsAttestations;
        lock.countDown();
    }

    @AfterEach
    public void shutdown() {
        hpmsAuthService.cleanup();
    }
}
