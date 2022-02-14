package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
class HPMSFetcherTest {

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
        Assertions.assertThrows(NullPointerException.class, () -> fetcher.retrieveSponsorInfo(null));
    }

    @Test
    void retrieveAttestationInfoNullCallback() {
        List<String> contracts = new ArrayList<>();
        Assertions.assertThrows(NullPointerException.class, () -> fetcher.retrieveAttestationInfo(null, contracts));
    }

    @Test
    void retrieveAttestationInfoNullContracts() {
        Assertions.assertDoesNotThrow(() -> fetcher.retrieveAttestationInfo(this::processAttestations, null));
    }

    @Test
    void retrieveAttestationInfo() {
        List<String> top6Contracts = retrieveTop6Contracts();
        lock = new CountDownLatch(1);
        fetcher.retrieveAttestationInfo(this::processAttestations, top6Contracts);
        waitForCallback();
        assertNotNull(attestations);
        assertFalse(attestations.isEmpty());
        // E4744 is not returned by the API
        assertEquals(NUM_CONTRACTS, attestations.size());
    }

    List<String> retrieveTop6Contracts() {
        fetcher.retrieveSponsorInfo(this::processOrgInfo);
        int retries = 0;
        do {
            waitForCallback();
        } while (orgs == null && retries++ < 20);

        assertNotNull(orgs);
        assertFalse(orgs.isEmpty());

        List<String> top6Contracts = extractTopContractIDs(NUM_CONTRACTS);
        assertNotNull(top6Contracts);
        assertFalse(top6Contracts.isEmpty());
        return top6Contracts;
    }

    private void waitForCallback() {
        try {
            lock.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private List<String> extractTopContractIDs(int limit) {
        List<String> retList = new ArrayList<>(3);
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
