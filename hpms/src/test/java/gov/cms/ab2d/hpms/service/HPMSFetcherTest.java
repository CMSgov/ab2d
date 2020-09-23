package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
public class HPMSFetcherTest {

    @Autowired
    private HPMSFetcher fetcher;

    private volatile HPMSOrganizations orgs;

    private CountDownLatch lock = new CountDownLatch(1);

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void fetchOrgs() {
        fetcher.retrieveSponsorInfo(this::processOrgInfo);
        try {
            lock.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        assertNotNull(orgs);
    }

    private void processOrgInfo(HPMSOrganizations orgInfo) {
        lock.countDown();
        orgs = orgInfo;
    }
}
