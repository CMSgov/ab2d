package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.contracts.hmsapi.HPMSAttestation;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "/application.properties")
@Testcontainers
class HPMSFetcherIT {

    final int NUM_CONTRACTS = 6;

    @Autowired
    private HPMSFetcher fetcher;

    @Autowired
    private HPMSAuthServiceImpl hpmsAuthService;

    private volatile List<HPMSOrganizationInfo> orgs;

    private volatile Set<HPMSAttestation> attestations;

    private CountDownLatch lock = new CountDownLatch(1);

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    void retrieveSponsorInfo() {
        retrieveTop6Contracts();
    }

    @Test
    void retrieveSponsorInfoNull() {
        Assertions.assertDoesNotThrow(() -> fetcher.retrieveSponsorInfo());
    }

    @Test
    void retrieveAttestationInfo() {
        List<String> top6Contracts = retrieveTop6Contracts();
        lock = new CountDownLatch(1);
        attestations = fetcher.retrieveAttestationInfo(top6Contracts);
        assertNotNull(attestations);
        assertFalse(attestations.isEmpty());
        // E4744 is not returned by the API
        assertTrue(attestations.size() <= NUM_CONTRACTS);
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
