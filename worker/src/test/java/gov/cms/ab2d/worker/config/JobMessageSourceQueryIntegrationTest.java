package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.FhirVersion.R4V3;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the polling SQL directly to ensure that the recovery/restart path doesn't accidentally
 * restart/recover jobs it's not supposed to. It checks that:
 *   - a SUBMITTED job with an int_lock row does not get picked up
 *   - an IN_PROGRESS job is recoverable only when it is prototype or pause/resume eligible
 */
@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
class JobMessageSourceQueryIntegrationTest {

    private static final String FLAG_KEY = "pause-resume.prototype.enabled";
    private static final int TTL_SECONDS = 60;
    private static final String CONTRACT = "Z0001";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK = new AB2DLocalstackContainer();

    @DynamicPropertySource
    static void sqsProps(DynamicPropertyRegistry registry) {
        registry.add("AWS_SQS_URL", LOCALSTACK::getSqsEndpoint);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobRepository jobRepository;

    // mocked purely so the worker context boots without a real coverage/BFD backend
    @MockitoBean
    private CoverageV3Service coverageV3Service;

    @MockitoBean
    private PatientClaimsProcessor patientClaimsProcessor;

    private JdbcTemplate jdbc;
    private final List<String> createdJobUuids = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        clearPrototypeFlag();
    }

    @AfterEach
    void tearDown() {
        for (String uuid : createdJobUuids) {
            jdbc.update("DELETE FROM int_lock WHERE lock_key = ?", uuid);
            jdbc.update("DELETE FROM ab2d.job_lease WHERE job_uuid = ?", uuid);
            jdbc.update("DELETE FROM job WHERE job_uuid = ?", uuid);
        }
        createdJobUuids.clear();
        clearPrototypeFlag();
    }

    @Test
    @DisplayName("The production poll query executes without error against postgres")
    void queryIsValidSql() {
        // proves the SQL compiles/runs
        assertDoesNotThrow(() -> poll());
    }

    @Test
    @DisplayName("A SUBMITTED job with no int_lock is eligible")
    void submittedWithoutLockIsEligible() {
        String uuid = createJob(JobStatus.SUBMITTED);
        assertTrue(poll().contains(uuid), "an unlocked SUBMITTED job should be polled");
    }

    @Test
    @DisplayName("A SUBMITTED job that already has an int_lock is skipped")
    void submittedWithIntLockIsSkipped() {
        String uuid = createJob(JobStatus.SUBMITTED);
        insertIntLock(uuid);
        assertFalse(poll().contains(uuid), "a SUBMITTED job with an int_lock row must not be polled");
    }

    @Test
    @DisplayName("An IN_PROGRESS job with a stale lease and the flag on is recoverable")
    void inProgressWithStaleLeaseAndFlagOnIsRecoverable() {
        enablePrototypeFlag();
        String uuid = createJob(JobStatus.IN_PROGRESS);
        insertLease(uuid, TTL_SECONDS * 2);
        assertTrue(poll().contains(uuid), "an IN_PROGRESS job with a dead lease should be recovered");
    }

    @Test
    @DisplayName("An IN_PROGRESS job with a fresh lease is not recovered")
    void inProgressWithFreshLeaseIsNotRecovered() {
        enablePrototypeFlag();
        String uuid = createJob(JobStatus.IN_PROGRESS);
        insertLease(uuid, 1);
        assertFalse(poll().contains(uuid), "a live worker's heartbeat should keep its job from being stolen");
    }

    @Test
    @DisplayName("An IN_PROGRESS job with no lease row at all is not recovered")
    void inProgressWithNoLeaseRowIsNotRecovered() {
        enablePrototypeFlag();
        String uuid = createJob(JobStatus.IN_PROGRESS);
        // no lease row means no recovery
        assertFalse(poll().contains(uuid),
                "an IN_PROGRESS job without a lease row must not be treated as recoverable");
    }

    @Test
    @DisplayName("A stale IN_PROGRESS job is not recovered while the prototype flag is off")
    void inProgressWithStaleLeaseButFlagOffIsNotRecovered() {
        // this test can be removed after integrating
        String uuid = createJob(JobStatus.IN_PROGRESS);
        insertLease(uuid, TTL_SECONDS * 2);
        assertFalse(poll().contains(uuid), "recovery must be gated behind the prototype flag");
    }

    /** Run the real query and return the set of job_uuids it selects. */
    private Set<String> poll() {
        List<Map<String, Object>> rows = jdbc.queryForList(JobMessageSource.buildQuery(TTL_SECONDS));
        return rows.stream().map(r -> String.valueOf(r.get("job_uuid"))).collect(Collectors.toSet());
    }

    private String createJob(JobStatus status) {
        Job job = new Job();
        // a real 36-char UUID so it matches the CHAR(36) int_lock.lock_key without padding surprises
        String uuid = UUID.randomUUID().toString();
        job.setJobUuid(uuid);
        job.setStatus(status);
        job.setStatusMessage("0%");
        job.setOrganization("query-test-org");
        job.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(R4V3);
        job.setContractNumber(CONTRACT);
        jobRepository.saveAndFlush(job);
        createdJobUuids.add(uuid);
        return uuid;
    }

    private void insertIntLock(String uuid) {
        jdbc.update("INSERT INTO int_lock (lock_key, region, created_date, expired_after) "
                + "VALUES (?, 'AB2D', now(), now() + make_interval(secs => 3600))", uuid);
    }

    private void insertLease(String uuid, int heartbeatAgeSeconds) {
        jdbc.update("INSERT INTO ab2d.job_lease (job_uuid, owner, token, heartbeat_at, clean_suspend_token) "
                + "VALUES (?, 'test-owner', 1, now() - make_interval(secs => ?), NULL)", uuid, heartbeatAgeSeconds);
    }

    private void enablePrototypeFlag() {
        clearPrototypeFlag();
        jdbc.update("INSERT INTO property.properties (id, key, value, created, modified) "
                + "SELECT COALESCE(MAX(id), 0) + 1, ?, 'true', now(), now() FROM property.properties", FLAG_KEY);
    }

    private void clearPrototypeFlag() {
        jdbc.update("DELETE FROM property.properties WHERE key = ?", FLAG_KEY);
    }
}
