package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test preformance of coverage service bulk read and write operations to make sure that
 * speed is preserved
 */
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

    static {
        System.setProperty("DB_USERNAME", "ab2d");
        System.setProperty("DB_PASSWORD", "ab2d");
        System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/ab2d");
    }

    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {

        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
        contractRepo.deleteAll();

        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract = dataSetup.setupContract(sponsor, "TST-123");

        period = dataSetup.createCoveragePeriod(contract, 1, 2020);
    }

    @AfterEach
    public void cleanUp() {
        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
        contractRepo.deleteAll();

        if (sponsor != null) {
            sponsorRepo.delete(sponsor);
        }
    }

    @Test
    void testing() {
        try {
            coverageRepo.findCoverageInformation(Collections.singletonList(106), List.of("test-999", "test-1000"));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        System.out.println("woohoo");
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
        List<Long> timings = insertData(inProgress, 1_000_000, 10, true);

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

        loadDBWithFakeData(2_000_000);

        int queries = 100;
        int threads = 16;
        int pageSize = 10000;
        int pages = 2_000_000 / 10000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Random random = new Random();

        List<Long> timing = new ArrayList<>(queries * threads);

        Runnable select = () -> {
            int i = 0;

            while (i < queries) {
                int page = random.nextInt(pages);
                Instant start = Instant.now();
                Page<String> pageRet = coverageRepo.findActiveBeneficiaryIds(Collections.singletonList(period), PageRequest.of(page, pageSize));
                List<String> content = pageRet.getContent();

                assertFalse(content.isEmpty());

                Instant stop = Instant.now();

                timing.add(Duration.between(start, stop).toNanos());
                i++;
            }
        };

        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            futures.add(executor.submit(select));
        }

        try {
            for (Future f : futures) {
                f.get();
            }
        } catch (Exception exception) {
            fail("failed to run all select threads", exception);
        }

        long sumNanos = timing.stream().mapToLong(i -> i).sum();
        long sumMillis = sumNanos / (1_000_000);

        System.out.println("Average millis per select: " + (sumMillis / (queries * threads)));
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

            insertData(inProgress, dataPoints, 1, false);
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

        String INSERT = "INSERT INTO coverage (bene_coverage_period_id, bene_coverage_search_event_id, beneficiary_id)" +
                " VALUES(?,?,?)";


        int i = 0;
        while (i < experiments) {

            Instant start = Instant.now();
            conductBatchInsert(supplier, inProgress.getId(), INSERT, dataPoints);
            Instant end = Instant.now();

            if (erase) {
                coverageRepo.deleteAll();
                coverageRepo.flush();
            }

            timings.add(Duration.between(start, end).toMillis());
            i++;
        }
        return timings;
    }

    private void conductBatchInsert(BeneficiaryIdSupplier supplier, long searchEventId, String INSERT, int dataPoints) {

        int written = 0;
        int chunkSize = 1000;

        while (written < dataPoints) {

            List<String> batch = new ArrayList<>(chunkSize);
            IntStream.iterate(0, i -> i + 1).limit(chunkSize).forEach(i -> batch.add(supplier.get()));

            coverageService.insertCoverage(period.getId(), searchEventId, batch);

            written += chunkSize;
        }
    }
}
