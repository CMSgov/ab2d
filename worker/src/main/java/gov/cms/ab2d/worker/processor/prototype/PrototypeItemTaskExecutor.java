package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import org.jspecify.annotations.NonNull;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Sets the round-robin queue category for the normal TaskExecutor.
 * Basically, it's a decorator that handles the "hack" portion of getting
 * the round-robin queue to recognize different categories.
 */
public class PrototypeItemTaskExecutor implements AsyncTaskExecutor {

    private final AsyncTaskExecutor delegate;
    private final String jobUuid;

    public PrototypeItemTaskExecutor(AsyncTaskExecutor delegate, String jobUuid) {
        this.delegate = delegate;
        this.jobUuid = jobUuid;
    }

    @Override
    public void execute(@NonNull Runnable task) {
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            delegate.execute(task);
        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }
    }

    @Override
    @NonNull
    public Future<?> submit(@NonNull Runnable task) {
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            return delegate.submit(task);
        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            return delegate.submit(task);
        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }
    }
}
