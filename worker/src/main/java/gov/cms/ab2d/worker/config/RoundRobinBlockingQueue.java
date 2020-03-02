package gov.cms.ab2d.worker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RoundRobinBlockingQueue<E> implements BlockingQueue<E> {
    // The list of categories
    private List<String> categories = new ArrayList<>();
    // The individual category queues
    private Map<String, LinkedList<E>> categoryQueues = new HashMap<>();
    // The current category index
    private int currentIndex = 0;
    // Main lock guarding all access
    private ReentrantLock lock;
    // Not empty condition on the lock
    private final Condition notEmpty;
    // Total number in the queues
    private long count = 0;

    public static final ThreadLocal<String> CATEGORY_HOLDER = new ThreadLocal<>();

    RoundRobinBlockingQueue() {
        log.info("Created Round Robin Blocking Queue");
        lock = new ReentrantLock();
        notEmpty = lock.newCondition();
    }

    // New code to add items
    private boolean add(String category, E e) {
        log.debug("Adding {} - {}", category, e);
        Assert.notNull(category, "Contract number required");
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (!categories.contains(category)) {
                categories.add(category);
                notEmpty.signal();
            }
            Queue<E> categoryQueue = categoryQueues.computeIfAbsent(category, k -> new LinkedList<>());
            count++;
            return categoryQueue.add(e);
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            categoryQueues.clear();
            categories.clear();
            count = 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (Map.Entry<String, LinkedList<E>> entry : categoryQueues.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(o)) {
                return true;
            }
        }
        return false;
    }

    public int drainTo(Collection<? super E> c) {
        long numToDrain = count;
        for (int i = 0; i < numToDrain; i++) {
            c.add(getNext());
        }
        return (int) numToDrain;
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        long numToDrain = maxElements;
        if (maxElements > count) {
            numToDrain = count;
        }
        for (int i = 0; i < numToDrain; i++) {
            c.add(getNext());
        }
        return (int) numToDrain;
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public boolean offer(E e) {
        return add(e);
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
        throw new UnsupportedOperationException("Not needed");
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
        return peekNext();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
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

    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean remove(Object o) {
        for (Map.Entry<String, LinkedList<E>> entry : categoryQueues.entrySet()) {
            if (entry.getValue().remove(o)) {
                count--;
                List entries = categoryQueues.get(entry.getKey());
                if (entries != null && entries.isEmpty()) {
                    categoryQueues.remove(entry.getKey());
                    categories.remove(entry.getKey());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not needed");
    }

    public int size() {
        return (int) count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            return getNext();
        } finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not needed");
    }

    public String toString() {
        if (categories.isEmpty()) {
            return "Empty";
        }
        StringBuilder builder = new StringBuilder();
        for (String category : categories) {
            builder.append(category).append(" with ").append(categoryQueues.get(category).size()).append(" Futures; ");
        }
        return builder.toString();
    }

    /**
     * Get the next round robin Object
     *
     * @return
     */
    private E getNext() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (categoryQueues == null || categories.isEmpty()) {
                return null;
            }
            if (currentIndex >= categories.size()) {
                currentIndex = 0;
            }
            String currentContract = categories.get(currentIndex);
            Queue<E> currentFuture = categoryQueues.get(currentContract);
            E val = currentFuture.poll();
            count--;
            if (currentFuture.size() == 0) {
                // No more requests so remove category. We don't have to increment index since we removed the item at the
                // current index
                categoryQueues.remove(currentFuture);
                categories.remove(currentContract);
            } else {
                currentIndex++;
            }
            return val;
        } finally {
            lock.unlock();
        }
    }

    private E peekNext() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (categoryQueues == null || categories.isEmpty()) {
                return null;
            }
            if (currentIndex >= categories.size()) {
                currentIndex = 0;
            }
            String currentContract = categories.get(currentIndex);
            return categoryQueues.get(currentContract).peek();
        } finally {
            lock.unlock();
        }
    }
}
