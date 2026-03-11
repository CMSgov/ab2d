package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;

import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest
@Testcontainers
@Import(AB2DSQSMockConfig.class)
class CoverageLockWrapperTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DCoveragePostgressqlContainer();

    @Autowired
    private CoverageLockWrapper coverageLockWrapper;

    // NOTE: Even with retry, this routinely fails
    @Test
    @Disabled("skipping testLockThreads() - testLockThreads() sometimes succeeds and sometimes fails even with multiple retries")
    void testLockThreadsWithRetry() {
        int maxAttempts=15;
        int attempt=0;
        boolean success=false;
        while (attempt < maxAttempts && !success) {
            try {
                testLockThreads();
                success=true;
            } catch (Throwable t) {
                log.error("testLockThreads() attempt {}/{} failed -- {}", attempt, maxAttempts, t.getMessage());
                attempt++;
            }
        }
        if (!success) {
            fail(String.format("testLockThreads() failed after %d attempts", maxAttempts));
        }
    }

    /**
     * The only way to trigger a lock error is if different threads are trying to use the lock at
     * the same time. This holds a lock for a period of time while another thread tries and fails
     * to get it and once the first thread is done, a third thread can then get the lock.
     *
     * @throws ExecutionException if there is an execution exception in the thread
     * @throws InterruptedException if a thread is interrupted
     */
    void testLockThreads() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        LockThread callable1 = new LockThread(coverageLockWrapper, 1);
        LockThread callable2 = new LockThread(coverageLockWrapper, 2);
        LockThread callable3 = new LockThread(coverageLockWrapper, 3);
        Future<Boolean> task1 = executor.submit(callable1);
        Future<Boolean> task2 = executor.submit(callable2);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> task1.isDone() && task2.isDone());
        boolean got1 = task1.get();
        boolean got2 = task2.get();
        assertTrue(got1 != got2);
        Future<Boolean> task3 = executor.submit(callable3);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(task3::isDone);
        assertTrue(task3.get());
    }
}
