package gov.cms.ab2d.worker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A custom implementation of {@link BlockingQueue} made to fit the needs of AB2D. Because we have
 * a single {@link java.util.concurrent.ThreadPoolExecutor} that handles all the tasks across
 * jobs, we've had a bit of a race condition for a while, which was being mitigated by throttling.
 * This queue implementation helps to solve that by introducing internal queues scoped to each
 * particular job (or a contract to be precise), and serving tasks to the surrounding
 * {@link java.util.concurrent.Executor}
 * in round-robin fashion.
 */
@Slf4j
public class RoundRobinBlockingQueue<E> implements BlockingQueue<E> {

    // The individual category queues
    private final Map<String, Deque<E>> categoryQueues =
            Collections.synchronizedMap(new LinkedHashMap<>());
    // The current category index
    private AtomicInteger currentIndex = new AtomicInteger();
    // Main lock guarding all access
    private final ReentrantLock lock = new ReentrantLock();
    // Not empty condition on the lock
    private final Condition notEmpty = lock.newCondition();

    private static final String OPERATION_NOT_NEEDED_STRING = "Not Needed";

    public static final ThreadLocal<String> CATEGORY_HOLDER = new ThreadLocal<>();

    // New code to add items
    private boolean add(String category, E e) {
        log.debug("Adding {} - {}", category, e);
        Assert.notNull(category, "Contract number must be set via CATEGORY_HOLDER prior to using this method");
        lock.lock();
        try {
            Deque<E> categoryQueue = categoryQueues.computeIfAbsent(category, k -> new LinkedList<>());
            categoryQueue.add(e);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        return this.add(CATEGORY_HOLDER.get(), e);
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            categoryQueues.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        lock.lock();
        try {
            for (Map.Entry<String, Deque<E>> entry : categoryQueues.entrySet()) {
                if (entry.getValue().contains(o)) {
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        if (c == this) {
            throw new IllegalArgumentException("Cannot drain to itself");
        }
        lock.lock();
        try {
            int size = size();
            for (Map.Entry<String, Deque<E>> entry : categoryQueues.entrySet()) {
                c.addAll(entry.getValue());
                entry.getValue().clear();
            }
            return size;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public boolean offer(E e) {
        try {
            return add(e);
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    @Override
    public E remove() {
        E next = getNext();
        if (next == null) {
            throw new NoSuchElementException("No elements left in the queue");
        }
        return next;
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public E peek() {
        return peekNext();
    }

    public E poll() {
        return getNext();
    }

    @Override
    public E element() {
        var next = peekNext();
        if (next == null) {
            throw new NoSuchElementException();
        }
        return next;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (size() == 0) {
                if (nanos <= 0L) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return getNext();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) {
        add(e);
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean remove(Object o) {
        lock.lock();
        try {
            for (Map.Entry<String, Deque<E>> entry : categoryQueues.entrySet()) {
                final Deque<E> queue = categoryQueues.get(entry.getKey());
                if (queue.remove(o)) {
                    if (queue.isEmpty()) {
                        categoryQueues.remove(entry.getKey());
                    }
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return categoryQueues.values().stream().map(queue -> queue.size())
                    .reduce(0, Integer::sum);
        } finally {
            lock.unlock();
        }
    }

    public int size(String category) {
        lock.lock();
        try {
            if (!categoryQueues.containsKey(category)) {
                return 0;
            }
            return categoryQueues.get(category).size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (size() == 0) {
                notEmpty.await();
            }
            return getNext();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException(OPERATION_NOT_NEEDED_STRING);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName()).append(": ");
        for (Map.Entry<String, Deque<E>> category : categoryQueues.entrySet()) {
            builder.append(category.getKey()).append(" with ").append(category.getValue().size())
                    .append(" Futures; ");
        }
        return builder.toString();
    }

    /**
     * Get the next round robin Object
     *
     * @return
     */
    private E getNext() {
        lock.lock();
        try {
            if (size() == 0) {
                return null;
            }
            if (currentIndex.get() >= categoryQueues.keySet().size()) {
                currentIndex.set(0);
            }
            String currentContract =
                    categoryQueues.keySet().stream().collect(Collectors.toList()).get(currentIndex.get());
            Deque<E> queue = categoryQueues.get(currentContract);
            E val = queue.poll();
            if (queue.size() == 0) {
                // No more requests so remove category. We don't have to increment index since we removed the item at the
                // current index
                categoryQueues.remove(currentContract);
            } else {
                currentIndex.incrementAndGet();
            }
            return val;
        } finally {
            lock.unlock();
        }
    }

    private E peekNext() {
        lock.lock();
        try {
            if (size() == 0) {
                return null;
            }
            if (currentIndex.get() >= categoryQueues.keySet().size()) {
                currentIndex.set(0);
            }
            String currentContract =
                    categoryQueues.keySet().stream().collect(Collectors.toList()).get(currentIndex.get());
            return categoryQueues.get(currentContract).peek();
        } finally {
            lock.unlock();
        }
    }
}