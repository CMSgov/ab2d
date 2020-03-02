package gov.cms.ab2d.worker.config;


import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@SpringBootTest(properties = {"pcp.core.pool.size=3" , "pcp.max.pool.size=20", "pcp.scaleToMax.time=20"})
@Testcontainers
@Slf4j
public class AutoScalingServiceTest {

    public static final int QUEUE_SIZE = 25;
    public static final int MAX_POOL_SIZE = 20;
    public static final int MIN_POOL_SIZE = 3;

    @Autowired
    private ThreadPoolTaskExecutor patientProcessorThreadPool;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();


    @BeforeEach
    public void init() {
        patientProcessorThreadPool.getThreadPoolExecutor().getQueue().clear();
    }

    @Test
    @DisplayName("Auto-scaling does not kick in when the queue remains empty")
    void autoScalingDoesNotKicksIn() throws InterruptedException {
        // Verify that initially the pool is sized at the minimums
        assertThat(patientProcessorThreadPool.getMaxPoolSize(), equalTo(3));
        assertThat(patientProcessorThreadPool.getCorePoolSize(), equalTo(3));

        // Auto-scaling should not kick in while the queue is empty
        Thread.sleep(7000);
        assertThat(patientProcessorThreadPool.getMaxPoolSize(), equalTo(3));
        assertThat(patientProcessorThreadPool.getCorePoolSize(), equalTo(3));

    }

    @Test
    @DisplayName("Auto-scaling kicks in and resizes the pool")
    void autoScalingKicksInAndResizes() throws InterruptedException {
        // Make the Executor busy.
        final List<Future> futures = new ArrayList<>();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set("TEST_CONTRACT");
        for (int i = 0; i < QUEUE_SIZE; i++) {
            futures.add(patientProcessorThreadPool.submit(sleepyRunnable()));
        }
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();

        // In approximately 20 seconds the Executor should grow to the max.
        var start = Instant.now();
        LinkedHashSet<Integer> metrics = new LinkedHashSet<>();
        for (int i = 0; i < 80; i++) {
            int size =
                    patientProcessorThreadPool.getThreadPoolExecutor().getMaximumPoolSize();
            metrics.add(size);
            log.info("Pool size: {}", size);
            if (size >= MAX_POOL_SIZE) {
                break;
            }
            Thread.sleep(500);
        }
        var end = Instant.now();

        // The pool should have grown to 20.
        assertThat(patientProcessorThreadPool.getMaxPoolSize(), equalTo(MAX_POOL_SIZE));

        // How do we actually verify that pool growth was in fact gradual and not instantaneous?
        // First, check that there was the expected time gap between auto scaling start & end
        assertThat(Duration.between(start, end).getSeconds(), greaterThanOrEqualTo(15L));

        // Then check that there were intermediate pool increases between 3 and MAX_POOL_SIZE.
        // Last metric taken should always be MAX_POOL_SIZE
        assertThat(new ArrayDeque<>(metrics).getLast(), equalTo(MAX_POOL_SIZE));

        // There are 3 intermediate metrics and 1 final metric
        assertThat(metrics.size(), greaterThanOrEqualTo(4));
        List<Integer> metricsList = new ArrayList<>(metrics);
        for (int i = 1; i < metricsList.size(); i++) {
            assertThat(metricsList.get(i - 1), lessThan(metricsList.get(i)));
        }

        // We need to make sure it does not grow beyond the max though. Let's sleep for a bit
        // and verify the size is still at the same max value.
        Thread.sleep(8000);
        assertThat(patientProcessorThreadPool.getMaxPoolSize(), equalTo(MAX_POOL_SIZE));

        // Clean up.
        futures.stream().forEach(future -> future.cancel(true));

        // Sleep for a bit to let auto scaling run another cycle. The max pool size should be
        // reverted
        // back to the original value after that.
        Thread.sleep(8000);
        assertThat(patientProcessorThreadPool.getMaxPoolSize(), equalTo(MIN_POOL_SIZE));
        assertThat(patientProcessorThreadPool.getThreadPoolExecutor().getActiveCount(), equalTo(0));
    }


    private Runnable sleepyRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    // NO-OP
                }
            }
        };
    }

}
