package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class CoverageLockWrapperTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private CoverageSearchRepository coverageSearchRepository;

    @Autowired
    private CoverageLockWrapper coverageLockWrapper;

    @Autowired
    private DataSetup dataSetup;

    /**
     * Verify that null is returned if there are no searches, a search there is one and verify that it
     * was deleted after it was searched.
     */
    @Test
    void getNextSearch() {
        assertTrue(coverageLockWrapper.getNextSearch().isEmpty());

        Sponsor sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        Contract contract1 = dataSetup.setupContract(sponsor, "c123");
        CoveragePeriod period1 = dataSetup.createCoveragePeriod(contract1, 10, 2020);
        CoverageSearch search1 = new CoverageSearch(null, period1, OffsetDateTime.now(), 0);
        CoverageSearch savedSearch1 = coverageSearchRepository.save(search1);
        Optional<CoverageSearch> returnedSearch = coverageLockWrapper.getNextSearch();
        assertEquals(savedSearch1.getPeriod().getMonth(), returnedSearch.get().getPeriod().getMonth());
        assertEquals(savedSearch1.getPeriod().getYear(), returnedSearch.get().getPeriod().getYear());
        assertTrue(coverageLockWrapper.getNextSearch().isEmpty());

        dataSetup.deleteCoveragePeriod(period1);
        dataSetup.deleteContract(contract1);
        dataSetup.deleteSponsor(sponsor);
    }

    /**
     * The only way to trigger a lock error is if different threads are trying to use the lock at
     * the same time. This holds a lock for a period of time while another thread tries and fails
     * to get it and once the first thread is done, a third thread can then get the lock.
     *
     * @throws ExecutionException if there is an execution exception in the thread
     * @throws InterruptedException if a thread is interrupted
     */
    @Test
    void testLockThreads() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        LockThread callable1 = new LockThread(coverageLockWrapper, 1);
        LockThread callable2 = new LockThread(coverageLockWrapper, 2);
        LockThread callable3 = new LockThread(coverageLockWrapper, 3);
        Future<Boolean> task1 = executor.submit(callable1);
        Future<Boolean> task2 = executor.submit(callable2);
        Thread.sleep(5000);
        Future<Boolean> task3 = executor.submit(callable3);
        boolean done1 = false;
        boolean done3 = false;
        while (!task1.isDone() || !task2.isDone() || !task3.isDone()) {
            if (task1.isDone() && task2.isDone() && !done1) {
                if (task1.get() == false && task2.get() == false) {
                    fail("Atleast one thread should get the lock");
                }
                if (task1.get() == true && task2.get() == true) {
                    fail("Both threads cannot get the lock");
                }
                if (!done1) {
                    done1 = true;
                }
            }
            if (task3.isDone()) {
                if (!done3) {
                    assertTrue(task3.get());
                    done3 = true;
                }
            }
        }
    }
}