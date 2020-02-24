package gov.cms.ab2d.worker.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import java.util.concurrent.*;

public class RoundRobinThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
    private ThreadPoolExecutor threadPoolExecutor;
    private final Object poolSizeMonitor = new Object();
    private int corePoolSize = 1;
    private int maxPoolSize = 2147483647;
    private int keepAliveSeconds = 60;
    private int queueCapacity = 2147483647;

    @NotNull
    protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        RoundRobinBlockingQueue<Runnable> queue = (RoundRobinBlockingQueue) this.createQueue(queueCapacity);
        ThreadPoolExecutor executor = new RoundRobinThreadPoolExecutor(this.getCorePoolSize(), this.getMaxPoolSize(),
                this.getKeepAliveSeconds(), TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);

        this.threadPoolExecutor = executor;
        return executor;
    }

    @NotNull
    @Override
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        return new RoundRobinBlockingQueue<>();
    }

    public Future<?> submitWithCategory(String category, Callable task) {
        RoundRobinThreadPoolExecutor executor = (RoundRobinThreadPoolExecutor) this.getThreadPoolExecutor();

        try {
            return executor.submitWithCategory(category, task);
        } catch (RejectedExecutionException var4) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, var4);
        }
    }

    public void execute(String category, Runnable task) {
        RoundRobinThreadPoolExecutor executor = (RoundRobinThreadPoolExecutor) this.getThreadPoolExecutor();

        try {
            executor.execute(task, category);
        } catch (RejectedExecutionException var4) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, var4);
        }
    }

    public int getMaxPoolSize() {
        synchronized(this.poolSizeMonitor) {
            return this.maxPoolSize;
        }
    }

    public void setCorePoolSize(int corePoolSize) {
        synchronized(this.poolSizeMonitor) {
            this.corePoolSize = corePoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setCorePoolSize(corePoolSize);
            }
        }
    }

    public void setMaxPoolSize(int maxPoolSize) {
        synchronized(this.poolSizeMonitor) {
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
            }
        }
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        synchronized(this.poolSizeMonitor) {
            this.keepAliveSeconds = keepAliveSeconds;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setKeepAliveTime((long)keepAliveSeconds, TimeUnit.SECONDS);
            }
        }
    }

    public int getKeepAliveSeconds() {
        synchronized(this.poolSizeMonitor) {
            return this.keepAliveSeconds;
        }
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
        return this.threadPoolExecutor;
    }

    public int getPoolSize() {
        return this.threadPoolExecutor == null ? this.corePoolSize : this.threadPoolExecutor.getPoolSize();
    }

    public int getActiveCount() {
        return this.threadPoolExecutor == null ? 0 : this.threadPoolExecutor.getActiveCount();
    }

    public void execute(Runnable task) {
        ThreadPoolExecutor executor = this.getThreadPoolExecutor();

        try {
            executor.execute(task);
        } catch (RejectedExecutionException var4) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, var4);
        }
    }
}
