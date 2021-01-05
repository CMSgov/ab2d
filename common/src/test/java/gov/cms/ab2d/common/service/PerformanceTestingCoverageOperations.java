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
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private CoverageServiceRepository coverageServiceRepo;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private DataSource dataSource;

    @Autowired CoverageSearchRepository coverageSearchRepository;

    private Contract contract;
    private CoveragePeriod period1;
    private CoveragePeriod period2;
    private CoveragePeriod period3;

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
// If you kill the integration test early uncomment this and comment the code below
//
//        deleteCoverage();
//        coverageSearchRepo.deleteAll();
//        coverageSearchEventRepo.deleteAll();
//        coveragePeriodRepo.deleteAll();
//        contractRepo.deleteAll();
//        if (sponsor != null) {
//            sponsorRepo.delete(sponsor);
//        }

// You will have to find the sponsor repo id manually
        contract = contractRepo.findContractByContractNumber("TST-12").get();
//        contract = contractRepo.findContractByContractNumber("TST-34").get();
//        contract = contractRepo.findContractByContractNumber("TST-56").get();
//        contract = contractRepo.findContractByContractNumber("TST-78").get();
//        contract = contractRepo.findContractByContractNumber("TST-90").get();

        period1 = coveragePeriodRepo.findByContractIdAndMonthAndYear(contract.getId(), 1, 2020).get();
        period2 = coveragePeriodRepo.findByContractIdAndMonthAndYear(contract.getId(), 2, 2020).get();
        period3 = coveragePeriodRepo.findByContractIdAndMonthAndYear(contract.getId(), 3, 2020).get();

//        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
//        contract = dataSetup.setupContract(sponsor, "TST-12");

//        period1 = dataSetup.createCoveragePeriod(contract, 1, 2020);
//        period2 = dataSetup.createCoveragePeriod(contract, 2, 2020);
//        period3 = dataSetup.createCoveragePeriod(contract, 3, 2020);

    }

    @AfterEach
    public void cleanUp() {
//        dataSetup.deleteCoverage();
//        coverageSearchRepo.deleteAll();
//        coverageSearchEventRepo.deleteAll();
//        coveragePeriodRepo.deleteAll();
//        contractRepo.delete(contract);
//        contractRepo.flush();
//
//        if (sponsor != null) {
//            sponsorRepo.delete(sponsor);
//            sponsorRepo.flush();
//        }
    }

    /**
     * Performance test for inserts, jack this number up really high
     */
    @Disabled("Performance test only for use manually")
    @DisplayName("Performance testing for inserts")
    @Test
    void insertPerformanceUsingConnection() {
        // Raise number of datapoints to stress database
        InsertionJob job = new InsertionJob(period1, dataSource, coverageService,
                1_000_000, 5, coverageSearchRepository);
        job.call();
    }

    /**
     * Performance test for inserts, jack this number up really high
     */
    @Disabled("Performance test only for use manually")
    @DisplayName("Performance testing for reads")
    @Test
    void readPerformanceWithJPAPaging() {

        int dataPoints = 20_000_000;
        int queries = 200;
        int threads = 10;
        int pageSize = 5000;
        int pages = dataPoints > pageSize ? dataPoints / pageSize : 1;

        List<CoveragePeriod> periods = new ArrayList<>();
        periods.add(period1);

// If you want to do a full (1hr long) performance test uncomment this code

        periods.add(period2);
        periods.add(period3);

//        Contract contract2 = dataSetup.setupContract(sponsor, "TST-34");
//        Contract contract3 = dataSetup.setupContract(sponsor, "TST-56");
//        Contract contract4 = dataSetup.setupContract(sponsor, "TST-78");
//        Contract contract5 = dataSetup.setupContract(sponsor, "TST-90");

//        periods.add(dataSetup.createCoveragePeriod(contract, 4, 2020));
//
//        periods.add(dataSetup.createCoveragePeriod(contract2, 1, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract2, 2, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract2, 3, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract2, 4, 2020));
//
//        periods.add(dataSetup.createCoveragePeriod(contract3, 1, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract3, 2, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract3, 3, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract3, 4, 2020));
//
//        periods.add(dataSetup.createCoveragePeriod(contract4, 1, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract4, 2, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract4, 3, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract4, 4, 2020));
//
//        periods.add(dataSetup.createCoveragePeriod(contract5, 1, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract5, 2, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract5, 3, 2020));
//        periods.add(dataSetup.createCoveragePeriod(contract5, 4, 2020));

//        loadDBWithFakeData(dataPoints, periods);

//        coverageServiceRepo.vacuumCoverage();

        System.out.println("Done loading data");

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Random random = new Random();

        final List<Long> timing = new ArrayList<>(queries * threads);

        Runnable select = () -> {

            String name = Thread.currentThread().getName();
            for (int queryNumber = 0; queryNumber < queries; queryNumber++) {
                int page = random.nextInt(pages);
                Instant start = Instant.now();
                CoveragePagingRequest pagingRequest = new CoveragePagingRequest(pageSize,
                        "test-" + page * pageSize + 1, period1.getId(), period2.getId());
                CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
                List<CoverageSummary> content = pagingResult.getCoverageSummaries();

                assertFalse(content.isEmpty());
                assertEquals(pageSize, content.size());
                Instant stop = Instant.now();

                if (queryNumber % 10 == 0) {
                    log.info("{} query {} took {} starting index {}", name, queryNumber, Duration.between(start, stop).toMillis(), page * pageSize);
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
            exception.printStackTrace();
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

    @Disabled("Performance test only for use manually")
    @DisplayName("delete previous search performance")
    @Test
    void deletePreviousSearch() {

        InsertionJob first = new InsertionJob(period1, dataSource, coverageService,
                100_000, 1, coverageSearchRepository);
        CoverageSearchEvent inProgress1 = first.call();

        coverageService.completeSearch(period1.getId(), "testing");

        InsertionJob second = new InsertionJob(period1, dataSource, coverageService,
                100_000, 1, coverageSearchRepository);
        CoverageSearchEvent inProgress2 = second.call();

        coverageService.completeSearch(period1.getId(), "testing");


        System.out.println("Records present before delete " + coverageServiceRepo.countBySearchEvent(inProgress1));

        Instant start = Instant.now();
        coverageService.deletePreviousSearch(period1.getId());
        Instant end = Instant.now();

        assertEquals(0, coverageServiceRepo.countBySearchEvent(inProgress1));
        assertEquals(100_000L, coverageServiceRepo.countBySearchEvent(inProgress2));
        System.out.println("Records present after delete " + coverageServiceRepo.countBySearchEvent(inProgress1));

        System.out.println("Time to delete previous search in milliseconds " + Duration.between(start, end).toMillis());
    }


    /**
     * Load fake data into the database for stress testing reads
     */
    private void loadDBWithFakeData(int dataPoints, List<CoveragePeriod> periods) {

        if (dataSetup.countCoverage() == 0) {

            ExecutorService executor = Executors.newFixedThreadPool(3);
            List<Future> insertions = new ArrayList<>();

            for (CoveragePeriod period : periods) {
                InsertionJob job = new InsertionJob(period, dataSource, coverageService,
                        dataPoints, 1, coverageSearchRepository);
                insertions.add(executor.submit(job));
            }

            try {
                for (Future f : insertions) {
                    f.get();
                }
            } catch (Exception exception) {
                fail("failed to run all select threads", exception);
            }
        }
    }
}
