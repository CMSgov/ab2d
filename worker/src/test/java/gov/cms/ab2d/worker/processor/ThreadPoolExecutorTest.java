package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.processor.stub.ProcessThread;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class ThreadPoolExecutorTest {
    @Autowired
    ProcessThread processThread;
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void runFakeThreads() throws InterruptedException {
        int numS000Threads = 1000;
        int numS001Threads = 100;
        int numS002Threads = 500;
        List<Future<?>> s0000Futures = new ArrayList<>();
        for (int i = 0; i < numS000Threads; i++) {
            s0000Futures.add(processThread.sleep("S0000"));
        }
        List<Future<?>> s0001Futures = new ArrayList<>();
        for (int i = 0; i < numS001Threads; i++) {
            s0001Futures.add(processThread.sleep("S0001"));
        }
        Thread.sleep(1000);
        // Assert that there is at least one S0001 thread done before all the S0000 threads are done
        assertTrue(s0001Futures.stream().anyMatch(Future::isDone));
        assertNotEquals(s0000Futures.stream().filter(Future::isDone).count(), numS000Threads);

        List<Future<?>> s0002Futures = new ArrayList<>();
        for (int i = 0; i < numS002Threads; i++) {
            s0002Futures.add(processThread.sleep("S0002"));
        }
        Thread.sleep(1000);
        assertTrue(s0002Futures.stream().anyMatch(Future::isDone));

        int totalThreads = numS000Threads + numS001Threads + numS002Threads;
        long doneThreads = 0;
        System.out.println("Total Threads: " + totalThreads);

        while (totalThreads > doneThreads) {
            doneThreads = s0000Futures.stream().map(Future::isDone).count();
            doneThreads += s0001Futures.stream().map(Future::isDone).count();
            doneThreads += s0002Futures.stream().map(Future::isDone).count();
            System.out.println((totalThreads - doneThreads) + " Left ");
            Thread.sleep(2000);
        }

        System.out.println("All done");
    }

}
