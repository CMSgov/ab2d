package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.job.service.JobCleanup;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.prototype.lease.JobLeaseRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.FhirVersion.R4V3;
import static gov.cms.ab2d.worker.TestUtil.getOpenRange;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi_V3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Shared harness for crash-recovery integration tests. Every recovery scenario runs the real Spring Batch
 * orchestration, the ndjson writer, and the restart function against a testcontainers postgres.
 * Coverage data access and claims fetch are mocked.
 *
 * The pickup poller is disabled, process() must be called directly.
 * Peer takeover is simulated by directly altering DB state.
 * SQL statements simulate a crashed job's batch metadata state.
 */
@SpringBootTest
@Testcontainers
@SpringIntegrationTest(noAutoStartup = {"inboundChannelAdapter", "*Source*"})
@TestPropertySource(properties = {
        // one partition at a time so there is a single, deterministic in-flight partition to interrupt
        "pause-resume.prototype.concurrency=1",
        "pause-resume.prototype.chunk-size=2",
        // high slowdown to avoid flakiness
        "pause-resume.prototype.item-delay-ms=300",
        "pause-resume.prototype.shutdown-await-ms=20000",
        "job.lock.ttl=60",
        "logging.level.gov.cms.ab2d.worker.processor.prototype=INFO",
        "logging.level.org.springframework.batch=INFO"
})
abstract class AbstractPrototypeRecoveryIntegrationTest extends JobCleanup {

    private static final Logger log = LoggerFactory.getLogger(AbstractPrototypeRecoveryIntegrationTest.class);

    protected static final String CONTRACT = "Z0001";
    protected static final int TOTAL_BENES = 20;
    protected static final int CHUNK_SIZE = 2;
    protected static final List<Long> BOUNDARIES = List.of(4L, 12L);
    protected static final String WORKER_STEP_LIKE = "ab2dPrototypeWorkerStep%";
    private static final Pattern PATIENT_REF = Pattern.compile("Patient/(\\d+)");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK = new AB2DLocalstackContainer();

    @DynamicPropertySource
    static void sqsProps(DynamicPropertyRegistry registry) {
        registry.add("AWS_SQS_URL", LOCALSTACK::getSqsEndpoint);
    }

    @Autowired
    protected PrototypeJobProcessor prototypeJobProcessor;

    @Autowired
    protected JobRepository jobRepository;

    @Autowired
    protected ContractServiceStub contractServiceStub;

    @Autowired
    protected ContractToContractCoverageMapping mapping;

    @Autowired
    protected PdpClientRepository pdpClientRepository;

    @Autowired
    protected DataSetup dataSetup;

    @Autowired
    protected SearchConfig searchConfig;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected JobLeaseRepository jobLease;

    @MockitoBean
    protected CoverageV3Service coverageV3Service;

    @MockitoBean
    protected PatientClaimsProcessor patientClaimsProcessor;

    @MockitoBean
    protected gov.cms.ab2d.eventclient.clients.SQSEventClient eventLogger;

    // every getEobBundleResources call appends the patient id here
    // duplicates are kept on purpose so a test can prove a resume did not redo already-committed work
    protected final List<Long> processedLog = Collections.synchronizedList(new ArrayList<>());

    protected JdbcTemplate jdbc;
    protected List<CoverageSummary> allBenes;

    @BeforeEach
    void setUpHarness() {
        jdbc = new JdbcTemplate(dataSource);
        processedLog.clear();

        Contract contract = new Contract();
        contract.setContractName(CONTRACT);
        contract.setContractNumber(CONTRACT);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contractServiceStub.updateContract(contract);
        dataSetup.queueForCleanup(contract);

        ContractForCoverageDTO coverageContract = mapping.map(contract);
        allBenes = LongStream.rangeClosed(1, TOTAL_BENES)
                .mapToObj(id -> new CoverageSummary(createIdentifierWithoutMbi_V3(id), coverageContract, List.of(getOpenRange())))
                .collect(Collectors.toList());

        // partitioning mock
        when(coverageV3Service.getMaxRowNumber(CONTRACT)).thenReturn((long) TOTAL_BENES);
        when(coverageV3Service.getPartitionBoundaryPatientIds(eq(CONTRACT), anyInt())).thenReturn(BOUNDARIES);

        // paging mock
        when(coverageV3Service.pageCoverageByPatientRange(eq(CONTRACT), anyLong(), anyLong(), any(), anyInt()))
                .thenAnswer(this::pageFor);

        // BFD mock
        when(patientClaimsProcessor.getEobBundleResources(any(), any())).thenAnswer(inv -> oneEobFor(inv.getArgument(1)));
    }

    @AfterEach
    void cleanUpHarness() {
        jobCleanup();
        dataSetup.cleanup();
        pdpClientRepository.deleteAll();
    }

    /** One page of coverage for the paging mock. Shared so a read-failure test can reuse it after throwing. */
    protected CoveragePagingResult pageFor(org.mockito.invocation.InvocationOnMock inv) {
        long start = inv.getArgument(1);
        long end = inv.getArgument(2);
        Optional<Long> cursor = inv.getArgument(3);
        long from = cursor.orElse(start);
        List<CoverageSummary> page = allBenes.stream()
                .filter(cs -> patientId(cs) > from && patientId(cs) <= end)
                .sorted((a, b) -> Long.compare(patientId(a), patientId(b)))
                .collect(Collectors.toList());
        // page size (1000) always covers a partition, so there is never a second page
        return new CoveragePagingResult(page, null);
    }

    /** One ExplanationOfBenefit per beneficiary, recording the call in processedLog. */
    protected List<IBaseResource> oneEobFor(CoverageSummary patient) {
        long id = patientId(patient);
        processedLog.add(id);
        org.hl7.fhir.r4.model.ExplanationOfBenefit eob = new org.hl7.fhir.r4.model.ExplanationOfBenefit();
        eob.setId("eob-" + id);
        eob.getPatient().setReference("Patient/" + id);
        return List.<IBaseResource>of(eob);
    }

    protected long patientId(CoverageSummary summary) {
        return summary.getIdentifiers().getPatientIdV3();
    }

    /** Persist a fresh SUBMITTED R4V3 job for test contract. */
    protected Job createSubmittedV3Job(String uuidPrefix) {
        return createSubmittedV3Job(uuidPrefix, CONTRACT);
    }

    /** Persist a fresh SUBMITTED R4V3 job for a given contract. */
    protected Job createSubmittedV3Job(String uuidPrefix, String contractNumber) {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(uuidPrefix + "-client");
        pdpClient.setOrganization(uuidPrefix + "-org");
        pdpClient.setEnabled(Boolean.TRUE);
        pdpClient = pdpClientRepository.saveAndFlush(pdpClient);
        dataSetup.queueForCleanup(pdpClient);

        Job job = new Job();
        job.setJobUuid(uuidPrefix + "-" + System.nanoTime());
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setOrganization(pdpClient.getOrganization());
        job.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(R4V3);
        job.setContractNumber(contractNumber);

        job = jobRepository.saveAndFlush(job);
        addJobForCleanup(job);
        return job;
    }

    /** A single-threaded daemon pool so a stuck run never keeps the test JVM alive. */
    protected ExecutorService newWorkerPool(String threadName) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start a background worker thread and block until at least one partition is done.
     * Returns the running worker so the caller can stop it, kill it, or let it finish.
     */
    protected RunningWorker startWorkerUntilOnePartitionDone(String uuid, String threadName) throws InterruptedException {
        ExecutorService pool = newWorkerPool(threadName);
        Future<Job> run = pool.submit(() -> prototypeJobProcessor.process(uuid));
        waitUntil(() -> completedPartitionCount(uuid) >= 1 || isTerminal(jobRepository.findByJobUuid(uuid)), 90);
        if (completedPartitionCount(uuid) < 1) {
            pool.shutdownNow();
            throw new AssertionError("no partition completed before the interrupt point, item delay might be too high");
        }
        if (isTerminal(jobRepository.findByJobUuid(uuid))) {
            pool.shutdownNow();
            throw new AssertionError("job finished before we could interrupt it, item delay might be too low");
        }
        return new RunningWorker(pool, run);
    }

    /** A standin for a running worker */
    protected record RunningWorker(ExecutorService pool, Future<Job> run) {

        void awaitReturn(long seconds) throws Exception {
            try {
                run.get(seconds, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }
        }

        /** Simulate a hard crash by terminating the process */
        void killHard() throws InterruptedException {
            pool.shutdownNow();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Bump the fence token, update the lease row. This should lock peers out of modifying the associated job.
     */
    protected long bumpLeaseOutOfBand(String uuid, String peerOwner) {
        int rows = jdbc.update("UPDATE ab2d.job_lease SET token = token + 1, owner = ?, "
                + "heartbeat_at = now(), clean_suspend_token = NULL WHERE job_uuid = ?", peerOwner, uuid);
        assertEquals(1, rows, "expected exactly one job_lease row to bump for " + uuid);
        return currentToken(uuid);
    }

    /** The current fence token for a job. */
    protected long currentToken(String uuid) {
        return jobLease.currentToken(uuid).orElseThrow(() -> new AssertionError("no lease row for " + uuid));
    }

    /** Getter for the clean suspend token. */
    protected Long cleanSuspendToken(String uuid) {
        return jdbc.queryForObject("SELECT clean_suspend_token FROM ab2d.job_lease WHERE job_uuid = ?",
                Long.class, uuid);
    }

    /** Modify the heartbeat timestamp so that this worker appears stale/dead */
    protected void ageHeartbeat(String uuid, int secondsOld) {
        int rows = jdbc.update("UPDATE ab2d.job_lease SET heartbeat_at = now() - make_interval(secs => ?) "
                + "WHERE job_uuid = ?", secondsOld, uuid);
        assertEquals(1, rows, "expected exactly one job_lease row to age for " + uuid);
    }

    /** True if the lease heartbeat is older than the TTL (the pickup input that authorizes a takeover). */
    protected boolean leaseStale(String uuid, int ttlSeconds) {
        return jobLease.find(uuid, ttlSeconds).orElseThrow().heartbeatStale();
    }

    /** get completed partitions with id and start_time tracking unique partitions */
    protected List<CompletedPartitionExecution> completedPartitions(String uuid) {
        return jdbc.query(
                "SELECT se.step_execution_id AS id, se.start_time AS st FROM batch_step_execution se "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = se.job_execution_id "
                        + "WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "  AND se.step_name LIKE ? AND se.status = 'COMPLETED' "
                        + "ORDER BY se.step_execution_id",
                (rs, rowNum) -> new CompletedPartitionExecution(rs.getLong("id"), rs.getTimestamp("st")),
                uuid, WORKER_STEP_LIKE);
    }

    protected long completedPartitionCount(String uuid) {
        return completedPartitions(uuid).size();
    }

    /** A COMPLETED partition step execution identified by id and start time (to prove a resume skipped it). */
    protected record CompletedPartitionExecution(long stepExecutionId, Timestamp startTime) {
    }

    protected List<String> workerStepStatuses(String uuid) {
        return jdbc.query(
                "SELECT se.status FROM batch_step_execution se "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = se.job_execution_id "
                        + "WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ? AND se.step_name LIKE ?",
                (rs, rowNum) -> rs.getString("status"),
                uuid, WORKER_STEP_LIKE);
    }

    /** Checks for a FenceLostException in the exit message, so we can prove what caused the failure */
    protected boolean fenceLostRecorded(String uuid) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM batch_step_execution se "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = se.job_execution_id "
                        + "WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "  AND se.step_name LIKE ? AND se.exit_message LIKE '%FenceLostException%'",
                Long.class, uuid, WORKER_STEP_LIKE);
        return count != null && count > 0;
    }

    /**
     * Make the batch metadata appear as though a worker crashed and left it behind. The heartbeat is stale,
     * the job execution and steps are in-progress and have no end time.
     */
    protected void forceHardCrashState(String uuid, String inflightStepStatus) {
        int steps = jdbc.update(
                "UPDATE batch_step_execution se SET status = ?, end_time = NULL, version = se.version + 1 "
                        + "FROM batch_job_execution je "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = je.job_execution_id "
                        + "WHERE se.job_execution_id = je.job_execution_id "
                        + "  AND p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "  AND se.step_name LIKE ? AND se.status <> 'COMPLETED'",
                inflightStepStatus, uuid, WORKER_STEP_LIKE);
        jdbc.update(
                "UPDATE batch_job_execution je SET status = 'STARTED', end_time = NULL, version = je.version + 1 "
                        + "FROM batch_job_execution_params p "
                        + "WHERE p.job_execution_id = je.job_execution_id "
                        + "  AND p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "  AND je.status <> 'COMPLETED'",
                uuid);
        jdbc.update("UPDATE ab2d.job_lease SET clean_suspend_token = NULL, "
                + "heartbeat_at = now() - make_interval(secs => 120) WHERE job_uuid = ?", uuid);
        jdbc.update("UPDATE job SET status = 'IN_PROGRESS' WHERE job_uuid = ?", uuid);
        log.info("forced hard-crash state for {}: {} in-flight worker step(s) -> {}, job execution -> STARTED, "
                + "heartbeat aged, clean-suspend cleared", uuid, steps, inflightStepStatus);
    }

    /** The winning per-partition component files */
    protected List<Path> finishedFiles(String uuid) {
        return ndjsonFilesIn(searchConfig.getFinishedDir(uuid));
    }

    /** The token-namespaced per-partition files in streaming directory */
    protected List<Path> streamingFiles(String uuid) {
        return ndjsonFilesIn(searchConfig.getStreamingDir(uuid));
    }

    /**
     * The assembled, downloadable data files.
     */
    protected List<Path> deliveredOutputFiles(String uuid) {
        return jobRootFiles(uuid, name -> name.endsWith(".ndjson.gz") && !name.endsWith("_error.ndjson.gz"));
    }

    /** The assembled, downloadable error files */
    protected List<Path> deliveredErrorFiles(String uuid) {
        return jobRootFiles(uuid, name -> name.endsWith("_error.ndjson.gz"));
    }

    /** Grabs all the output files */
    private List<Path> jobRootFiles(String uuid, java.util.function.Predicate<String> nameMatch) {
        File[] files = searchConfig.getFinishedDir(uuid).getParentFile()
                .listFiles((d, name) -> nameMatch.test(name));
        if (files == null) {
            return List.of();
        }
        return List.of(files).stream().map(File::toPath).sorted().collect(Collectors.toList());
    }

    private List<Path> ndjsonFilesIn(File dir) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".ndjson"));
        if (files == null) {
            return List.of();
        }
        return List.of(files).stream().map(File::toPath).sorted().collect(Collectors.toList());
    }

    /** Read the lines of an ndjson file, unzipping it if it's compressed */
    protected List<String> linesOf(Path file) throws IOException {
        if (file.getFileName().toString().endsWith(".gz")) {
            try (var in = new java.util.zip.GZIPInputStream(Files.newInputStream(file));
                 var reader = new java.io.BufferedReader(
                         new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.toList());
            }
        }
        return Files.readAllLines(file);
    }

    /** Every beneficiary id referenced across the given ndjson files (compressed or not). */
    protected List<Long> benesIn(List<Path> files) throws IOException {
        List<Long> ids = new ArrayList<>();
        for (Path file : files) {
            for (String line : linesOf(file)) {
                Matcher m = PATIENT_REF.matcher(line);
                if (m.find()) {
                    ids.add(Long.parseLong(m.group(1)));
                }
            }
        }
        return ids;
    }

    /** How many JobOutput manifest rows exist for a job. */
    protected int jobOutputRowCount(String uuid) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM job_output jo JOIN job j ON j.id = jo.job_id WHERE j.job_uuid = ?",
                Long.class, uuid);
        return count == null ? 0 : count.intValue();
    }

    /** The file paths registered as JobOutput manifest rows for a job. */
    protected List<String> jobOutputFilePaths(String uuid) {
        return jdbc.query(
                "SELECT jo.file_path FROM job_output jo JOIN job j ON j.id = jo.job_id WHERE j.job_uuid = ? "
                        + "ORDER BY jo.file_path",
                (rs, rowNum) -> rs.getString("file_path"), uuid);
    }

    protected static final Set<Long> ALL_BENES =
            LongStream.rangeClosed(1, TOTAL_BENES).boxed().collect(Collectors.toUnmodifiableSet());

    /**
     * Assert that the output contains every bene exactly once, no duplicates, nobody missing.
     */
    protected void assertEveryBeneExactlyOnceInOutput(String uuid) throws IOException {
        List<Long> delivered = benesIn(deliveredOutputFiles(uuid));
        assertEquals(ALL_BENES.size(), delivered.size(),
                "expected exactly " + ALL_BENES.size() + " beneficiaries in the output, saw " + delivered.size()
                        + " (duplicate=" + duplicates(delivered) + ")");
        assertEquals(ALL_BENES, new HashSet<>(delivered),
                "every beneficiary should appear exactly once in the output; missing="
                        + missing(ALL_BENES, new HashSet<>(delivered)));
    }

    protected static boolean isTerminal(Job job) {
        JobStatus status = job.getStatus();
        return status == JobStatus.SUCCESSFUL || status == JobStatus.FAILED || status == JobStatus.CANCELLED;
    }

    protected static Set<Long> missing(Set<Long> expected, Set<Long> actual) {
        Set<Long> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    protected static List<Long> duplicates(List<Long> ids) {
        Set<Long> seen = new HashSet<>();
        return ids.stream().filter(id -> !seen.add(id)).distinct().collect(Collectors.toList());
    }

    protected void waitUntil(BooleanSupplier condition, long timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("condition not met within " + timeoutSeconds + "s");
    }
}
