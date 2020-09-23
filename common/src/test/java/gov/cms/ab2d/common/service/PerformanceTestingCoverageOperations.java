package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.DataSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test performance of coverage service bulk read and write operations to make sure that
 * speed is preserved.
 *
 * To perform these tests run "docker-compose up db" and make sure the container started
 * is exposed on port 5432. Then manually start each test.
 *
 * Most of the variables in the test are hard coded and can be changed to make the performance tests
 * more rigorous or simpler.
 */
@Slf4j
@SpringBootTest
@TestPropertySource(locations = "/application.common.properties")
class PerformanceTestingCoverageOperations {

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private SponsorRepository sponsorRepo;

    @Autowired
    private CoverageRepository coverageRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private DataSource dataSource;

    private Sponsor sponsor;
    private Contract contract;
    private CoveragePeriod period;

    /*
     * Set the database connection information including the username, password, database name,
     * and most importantly port that the database is present on.
     *
     * Only use this when using the docker-compose db container for testing. If using testcontainers
     * this is unnecessary.
     */
    static {
        System.setProperty("DB_USERNAME", "ab2d");
        System.setProperty("DB_PASSWORD", "ab2d");
        System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/ab2d");
    }

    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {
// If you kill the integration test early uncomment this
//        coverageRepo.deleteAll();
//        coverageSearchEventRepo.deleteAll();
//        coveragePeriodRepo.deleteAll();
//        contractRepo.deleteAll();

        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract = dataSetup.setupContract(sponsor, "TST-123");

        period = dataSetup.createCoveragePeriod(contract, 1, 2020);
    }

    @AfterEach
    public void cleanUp() {
        deleteCoverage();
        coverageSearchEventRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
        contractRepo.deleteAll();

        if (sponsor != null) {
            sponsorRepo.delete(sponsor);
        }
    }

    /**
     * Performance test for inserts, jack this number up really high
     */
    @Disabled
    @DisplayName("Performance testing for inserts")
    @Test
    void insertPerformanceUsingConnection() {

        CoverageSearchEvent inProgress = new CoverageSearchEvent();
        inProgress.setOldStatus(JobStatus.SUBMITTED);
        inProgress.setNewStatus(JobStatus.IN_PROGRESS);
        inProgress.setDescription("testing");
        inProgress.setCoveragePeriod(period);

        coverageSearchEventRepo.saveAndFlush(inProgress);

        // Raise datapoints to stress database
        List<Long> timings = insertData(inProgress, 1_000_000, 5, true);

        long averageTime = timings.stream().reduce(0L, Long::sum) / timings.size();
        System.out.println("Average milliseconds " + averageTime);
        System.out.println("Times " + timings.stream().map(Object::toString).collect(joining(", ")));
    }

    /**
     * Performance test for inserts, jack this number up really high
     */
    @Disabled
    @DisplayName("Performance testing for reads")
    @Test
    void readPerformanceWithJPAPaging() {



        int dataPoints = 5_000_000;
        int queries = 200;
        int threads = 6;
        int pageSize = 5000;
        int pages = dataPoints / pageSize;

        loadDBWithFakeData(dataPoints);

        System.out.println("Done loading data");

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Random random = new Random();

        final List<Long> timing = new ArrayList<>(queries * threads);

        Runnable select = () -> {

            String name = Thread.currentThread().getName();
            for (int queryNumber = 0; queryNumber < queries; queryNumber++) {
                int page = random.nextInt(pages);
                Instant start = Instant.now();
                List<CoverageSummary> content = coverageService.pageCoverage(page, pageSize, singletonList(period.getId()));

                assertFalse(content.isEmpty());
                assertEquals(pageSize, content.size());
                Instant stop = Instant.now();

                if (queryNumber % 10 == 0) {
                    log.info("{} query {} took {}", name, queryNumber, Duration.between(start, stop).toNanos());
                }
                timing.add(Duration.between(start, stop).toNanos());
            }
        };

        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(select));
        }

        try {
            for (Future f : futures) {
                f.get();
            }
        } catch (Exception exception) {
            fail("failed to run all select threads", exception);
        }

        for (int i = 0; i < timing.size(); i++) {
            timing.set(i, timing.get(i) / 1_000_000);
        }
        timing.sort((a, b) -> Long.valueOf(b - a).intValue());

        long sumMillis = timing.stream().mapToLong(i -> i).sum();

        System.out.println("Average millis per select: " + (sumMillis / (queries * threads)));
        System.out.println("Max times " + timing.stream().limit(30).map(Objects::toString).collect(joining(", ")));
    }

    @Disabled
    @DisplayName("delete previous search performance")
    @Test
    void deletePreviousSearch() {

        coverageService.submitCoverageSearch(period.getId(), "testing");
        CoverageSearchEvent inProgress1 = coverageService.startCoverageSearch(period.getId(), "testing");

        coverageSearchEventRepo.saveAndFlush(inProgress1);
        insertData(inProgress1, 100_000, 1, false);

        coverageService.completeCoverageSearch(period.getId(), "testing");

        coverageService.submitCoverageSearch(period.getId(), "testing");
        CoverageSearchEvent inProgress2 = coverageService.startCoverageSearch(period.getId(), "testing");

        coverageSearchEventRepo.saveAndFlush(inProgress2);
        insertData(inProgress2, 100_000, 1, false);

        coverageService.completeCoverageSearch(period.getId(), "testing");


        System.out.println("Records present before delete " + coverageRepo.countByCoverageSearchEvent(inProgress1));

        Instant start = Instant.now();
        coverageService.deletePreviousSearch(period.getId());
        Instant end = Instant.now();

        System.out.println("Records present after delete " + coverageRepo.countByCoverageSearchEvent(inProgress1));

        System.out.println("Time to delete previous search in milliseconds " + Duration.between(start, end).toMillis());
    }


    /**
     * Load fake data into the database for stress testing reads
     */
    private void loadDBWithFakeData(int dataPoints) {

        if (coverageRepo.count() == 0) {
            CoverageSearchEvent inProgress = new CoverageSearchEvent();
            inProgress.setOldStatus(JobStatus.SUBMITTED);
            inProgress.setNewStatus(JobStatus.IN_PROGRESS);
            inProgress.setDescription("testing");
            inProgress.setCoveragePeriod(period);

            coverageSearchEventRepo.saveAndFlush(inProgress);

            List<Long> timings = insertData(inProgress, dataPoints, 1, false);
            System.out.println("Insertion took " + timings.get(0));
        }
    }

    public static class BeneficiaryIdSupplier implements Supplier<String> {

        private int id = 0;

        public BeneficiaryIdSupplier() {}

        public String get() {
            return "test-" + id++;
        }
    }

    private List<Long> insertData(CoverageSearchEvent inProgress, int dataPoints, int experiments, boolean erase) {
        BeneficiaryIdSupplier supplier = new BeneficiaryIdSupplier();

        List<Long> timings = new ArrayList<>();

        int i = 0;
        while (i < experiments) {

            Instant start = Instant.now();
            conductBatchInsert(supplier, inProgress.getId(), dataPoints);
            Instant end = Instant.now();

            if (erase) {
                deleteCoverage();
            }

            timings.add(Duration.between(start, end).toMillis());
            i++;
        }
        return timings;
    }

    private void conductBatchInsert(BeneficiaryIdSupplier supplier, long searchEventId, int dataPoints) {

        int written = 0;
        int chunkSize = 50000;

        while (written < dataPoints) {

            List<String> batch = new ArrayList<>(chunkSize);
            IntStream.iterate(0, i -> i + 1).limit(chunkSize).forEach(i -> batch.add(supplier.get()));

            coverageService.insertCoverage(period.getId(), searchEventId, batch);

            written += chunkSize;
        }
    }

    private void deleteCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM coverage")) {
            statement.execute();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
