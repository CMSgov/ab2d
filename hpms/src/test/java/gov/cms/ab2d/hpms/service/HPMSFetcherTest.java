package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
public class HPMSFetcherTest {

    @Autowired
    private HPMSFetcher fetcher;

    @Autowired
    private HPMSAuthServiceImpl hpmsAuthService;

    private volatile HPMSOrganizations orgs;

    private volatile HPMSAttestationsHolder attestations;

    private CountDownLatch lock = new CountDownLatch(1);

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void fetchTheSlippers() {
        fetcher.retrieveSponsorInfo(this::processOrgInfo);
        waitForCallback();

        assertNotNull(orgs);
        assertFalse(orgs.getOrgs().isEmpty());

        final int NUM_CONTRACTS = 3;
        List<String> top3Contracts = extractTopContractIDs(NUM_CONTRACTS);
        assertNotNull(top3Contracts);
        assertFalse(top3Contracts.isEmpty());

        lock = new CountDownLatch(1);
        fetcher.retrieveAttestationInfo(this::processAttestations, top3Contracts);
        waitForCallback();
        assertNotNull(attestations);
        assertFalse(attestations.getContracts().isEmpty());
        assertEquals(NUM_CONTRACTS, attestations.getContracts().size());
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
        final int sponsorSize = orgs.getOrgs().size();
        for (int idx = 0; idx < limit && idx < sponsorSize; idx++) {
            retList.add(orgs.getOrgs().get(idx).getContractId());
        }
        return retList;
    }

    private void processOrgInfo(HPMSOrganizations orgInfo) {
        orgs = orgInfo;
        lock.countDown();
    }

    private void processAttestations(HPMSAttestationsHolder hpmsAttestationsHolder) {
        attestations = hpmsAttestationsHolder;
        lock.countDown();
    }

    @AfterEach
    public void shutdown() {
        hpmsAuthService.cleanup();
    }
}
