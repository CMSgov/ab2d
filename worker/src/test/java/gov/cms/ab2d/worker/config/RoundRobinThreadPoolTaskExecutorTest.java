package gov.cms.ab2d.worker.config;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundRobinThreadPoolTaskExecutorTest {
    static final String THREAD_NAME_PREFIX = "test-";

    private RoundRobinThreadPoolTaskExecutor executor;

    private volatile Object outcome;

    @BeforeEach
    public void initExecutor() {
        executor = buildExecutor();
    }

    protected RoundRobinThreadPoolTaskExecutor buildExecutor() {
        RoundRobinThreadPoolTaskExecutor executor = new RoundRobinThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setMaxPoolSize(1);
        executor.afterPropertiesSet();
        return executor;
    }

    @AfterEach
    public void shutdownExecutor() throws Exception {
        if (executor instanceof DisposableBean) {
            ((DisposableBean) executor).destroy();
        }
    }

    @Test
    public void testGettersSetters() {
        assertEquals(1, executor.getMaxPoolSize());
        assertEquals(0, executor.getActiveCount());
        executor.setCorePoolSize(1);
        assertEquals(1, executor.getCorePoolSize());
        executor.setKeepAliveSeconds(10);
        assertEquals(10, executor.getKeepAliveSeconds());
        executor.setQueueCapacity(2);
        assertEquals(0, executor.getPoolSize());
    }

    @Test
    public void executeRunnable() {
        TestTask task = new TestTask(1);
        executor.execute(task);
        await(task);
        assertThreadNamePrefix(task);
    }

    @Test
    public void executeFailingRunnable() {
        TestTask task = new TestTask(0);
        executor.execute(task);
        // nothing to assert
    }

    @Test
    public void submitRunnable() throws Exception {
        TestTask task = new TestTask(1);
        Future<?> future = executor.submit(task);
        Object result = future.get(1000, TimeUnit.MILLISECONDS);
        assertThat(result).isNull();
        assertThreadNamePrefix(task);
    }

    @Test
    public void submitFailingRunnable() throws Exception {
        TestTask task = new TestTask(0);
        Future<?> future = executor.submit(task);
        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
                future.get(1000, TimeUnit.MILLISECONDS));
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void submitRunnableWithGetAfterShutdown() throws Exception {
        Future<?> future1 = ((RoundRobinThreadPoolTaskExecutor) executor).submitWithCategory("CAT", new TestCallable(-1));
        Future<?> future2 = ((RoundRobinThreadPoolTaskExecutor) executor).submitWithCategory("CAT", new TestCallable(-1));
        shutdownExecutor();
        assertThatExceptionOfType(CancellationException.class).isThrownBy(() -> {
            future1.get(1000, TimeUnit.MILLISECONDS);
            future2.get(1000, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    public void submitFailingListenableRunnable() throws Exception {
        TestTask task = new TestTask(0);
        ListenableFuture<?> future = executor.submitListenable(task);
        future.addCallback(result -> outcome = result, ex -> outcome = ex);

        Awaitility.await()
                .dontCatchUncaughtExceptions()
                .atMost(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .until(() -> future.isDone() && outcome != null);
        assertThat(outcome.getClass()).isSameAs(RuntimeException.class);
    }

    @Test
    public void submitCallable() throws Exception {
        TestCallable task = new TestCallable(1);
        Future<String> future = executor.submit(task);
        String result = future.get(1000, TimeUnit.MILLISECONDS);
        assertThat(result.substring(0, THREAD_NAME_PREFIX.length())).isEqualTo(THREAD_NAME_PREFIX);
    }

    @Test
    public void submitFailingCallable() throws Exception {
        TestCallable task = new TestCallable(0);
        Future<String> future = executor.submit(task);
        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
                future.get(1000, TimeUnit.MILLISECONDS));
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void submitCallableWithGetAfterShutdown() throws Exception {
        Future<?> future1 = executor.submitWithCategory("ABC", new TestCallable(-1));
        Future<?> future2 = executor.submitWithCategory("DEF", new TestCallable(-1));
        shutdownExecutor();

        try {
            future1.get(1000, TimeUnit.MILLISECONDS);
        }
        catch (Exception ex) {
            /* ignore */
        }
        Awaitility.await()
                .atMost(4, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        assertThatExceptionOfType(CancellationException.class).isThrownBy(() ->
                                future2.get(1000, TimeUnit.MILLISECONDS)));
    }

    @Test
    public void submitListenableCallable() throws Exception {
        TestCallable task = new TestCallable(1);
        // Act
        ListenableFuture<String> future = executor.submitListenable(task);
        future.addCallback(result -> outcome = result, ex -> outcome = ex);
        // Assert
        Awaitility.await()
                .atMost(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .until(() -> future.isDone() && outcome != null);
        assertThat(outcome.toString().substring(0, THREAD_NAME_PREFIX.length())).isEqualTo(THREAD_NAME_PREFIX);
    }

    @Test
    public void submitFailingListenableCallable() throws Exception {
        TestCallable task = new TestCallable(0);
        // Act
        ListenableFuture<String> future = executor.submitListenable(task);
        future.addCallback(result -> outcome = result, ex -> outcome = ex);
        // Assert
        Awaitility.await()
                .dontCatchUncaughtExceptions()
                .atMost(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .until(() -> future.isDone() && outcome != null);
        assertThat(outcome.getClass()).isSameAs(RuntimeException.class);
    }

    @Test
    public void submitListenableCallableWithGetAfterShutdown() throws Exception {
        ListenableFuture<?> future1 = executor.submitListenable("CAT", new TestCallable(-1));
        ListenableFuture<?> future2 = executor.submitListenable("CAT", new TestCallable(-1));
        shutdownExecutor();
        assertThatExceptionOfType(CancellationException.class).isThrownBy(() -> {
            future1.get(1000, TimeUnit.MILLISECONDS);
            future2.get(1000, TimeUnit.MILLISECONDS);
        });
    }


    private void assertThreadNamePrefix(TestTask task) {
        assertThat(task.lastThread.getName().substring(0, THREAD_NAME_PREFIX.length())).isEqualTo(THREAD_NAME_PREFIX);
    }

    private void await(TestTask task) {
        await(task.latch);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        assertThat(latch.getCount()).as("latch did not count down,").isEqualTo(0);
    }


    private static class TestTask implements Runnable {

        private final int expectedRunCount;

        private final AtomicInteger actualRunCount = new AtomicInteger();

        private final CountDownLatch latch;

        private Thread lastThread;

        TestTask(int expectedRunCount) {
            this.expectedRunCount = expectedRunCount;
            this.latch = (expectedRunCount > 0 ? new CountDownLatch(expectedRunCount) : null);
        }

        @Override
        public void run() {
            lastThread = Thread.currentThread();
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException ex) {
            }
            if (expectedRunCount >= 0) {
                if (actualRunCount.incrementAndGet() > expectedRunCount) {
                    throw new RuntimeException("intentional test failure");
                }
                latch.countDown();
            }
        }
    }

    private static class TestCallable implements Callable<String> {

        private final int expectedRunCount;

        private final AtomicInteger actualRunCount = new AtomicInteger();

        TestCallable(int expectedRunCount) {
            this.expectedRunCount = expectedRunCount;
        }

        public String call() throws Exception {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException ex) {
            }
            if (expectedRunCount >= 0) {
                if (actualRunCount.incrementAndGet() > expectedRunCount) {
                    throw new RuntimeException("intentional test failure");
                }
            }
            return Thread.currentThread().getName();
        }
    }
}