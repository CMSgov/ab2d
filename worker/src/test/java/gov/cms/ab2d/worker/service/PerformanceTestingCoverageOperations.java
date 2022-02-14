package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.service.InsertionJob;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
@Disabled
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
    private CoverageDataSetup coverageDataSetup;

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
// If you kill the integration test early uncomment this to clean things out before starting the test
//
//        deleteCoverage();
//        coverageSearchRepo.deleteAll();
//        coverageSearchEventRepo.deleteAll();
//        coveragePeriodRepo.deleteAll();
//        contractRepo.deleteAll();
//        if (sponsor != null) {
//            sponsorRepo.delete(sponsor);
//        }

        contract = dataSetup.setupContract("TST-12", AB2D_EPOCH.toOffsetDateTime());
        period1 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 1, 2020);
        period2 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 2, 2020);
        period3 = coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 3, 2020);

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
                1_000_000, 1, coverageSearchRepository);
        job.call();
    }

    /**
     * Performance test for inserts, jack this number up really high
     */
    @Disabled("Performance test only for use manually")
    @DisplayName("Performance testing for reads")
    @Test
    void readPerformanceWithJPAPaging() {

        int dataPoints = 3_000_000;
        int queries = 200;
        int threads = 10;
        int pageSize = 5000;
        int pages = dataPoints > pageSize ? dataPoints / pageSize : 1;

        Contract contract2 = dataSetup.setupContract("TST-34", AB2D_EPOCH.toOffsetDateTime());

        List<CoveragePeriod> periods = new ArrayList<>();

        // Test to make sure there are no problems adding two contracts
        periods.add(period1);
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 1, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 1, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 1, 2021));

        periods.add(period2);
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 2, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 2, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 2, 2021));

        periods.add(period3);
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 4, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 5, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 6, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 7, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 8, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 9, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 10, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 11, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 12, 2020));

        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 3, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 4, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 5, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 6, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 7, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 8, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 9, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 10, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 11, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract.getContractNumber(), 12, 2021));

        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 3, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 4, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 5, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 6, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 7, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 8, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 9, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 10, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 11, 2020));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 12, 2020));

        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 3, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 4, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 5, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 6, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 7, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 8, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 9, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 10, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 11, 2021));
        periods.add(coverageDataSetup.createCoveragePeriod(contract2.getContractNumber(), 12, 2021));


//        Contract contract3 = dataSetup.setupContract("TST-56");
//        Contract contract4 = dataSetup.setupContract("TST-78");
//        Contract contract5 = dataSetup.setupContract("TST-90");

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

        loadDBWithFakeData(dataPoints, periods);

        coverageServiceRepo.vacuumCoverage();

        System.out.println("Done loading data");

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Random random = new Random();

        // Use if you want to performance test queries all in one go.
        // The previous code will populate a database for reuse.
        // This code will exercise the coverage table by randomly paging through it

//        List<Integer> periodIds = coveragePeriodRepo.findAllByContractId(contract.getId())
//                .stream().map(CoveragePeriod::getId).collect(Collectors.toList());
//
//        final List<Long> timing = new ArrayList<>(queries * threads);
//
//        Runnable select = () -> {
//
//            String name = Thread.currentThread().getName();
//            for (int queryNumber = 0; queryNumber < queries; queryNumber++) {
//                int page = random.nextInt(pages);
//                Instant start = Instant.now();
//                CoveragePagingRequest pagingRequest = new CoveragePagingRequest(pageSize,
//                        "test-" + page * pageSize + 1, contract.getContractNumber(),
//                        periodIds);
//                CoveragePagingResult pagingResult = coverageService.pageCoverage(pagingRequest);
//                List<CoverageSummary> content = pagingResult.getCoverageSummaries();
//
//                assertFalse(content.isEmpty());
//                assertEquals(pageSize, content.size());
//                Instant stop = Instant.now();
//
//                if (queryNumber % 10 == 0) {
//                    log.info("{} query {} took {} starting index {}", name, queryNumber, Duration.between(start, stop).toMillis(), page * pageSize);
//                }
//                timing.add(Duration.between(start, stop).toNanos());
//            }
//        };
//
//        List<Future> futures = new ArrayList<>();
//        for (int i = 0; i < threads; i++) {
//            futures.add(executor.submit(select));
//        }
//
//        try {
//            for (Future f : futures) {
//                f.get();
//            }
//        } catch (Exception exception) {
//            exception.printStackTrace();
//            fail("failed to run all select threads", exception);
//        }
//
//        for (int i = 0; i < timing.size(); i++) {
//            timing.set(i, timing.get(i) / 1_000_000);
//        }
//        timing.sort((a, b) -> Long.valueOf(b - a).intValue());
//
//        long sumMillis = timing.stream().mapToLong(i -> i).sum();
//
//        System.out.println("Average millis per select: " + (sumMillis / (queries * threads)));
//        System.out.println("Max times " + timing.stream().limit(30).map(Objects::toString).collect(joining(", ")));
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
        coverageServiceRepo.deletePreviousSearches(period1, 1);
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

            ExecutorService executor = Executors.newFixedThreadPool(1);
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
