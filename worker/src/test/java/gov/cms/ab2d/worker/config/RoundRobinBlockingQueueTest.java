package gov.cms.ab2d.worker.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinBlockingQueueTest {


    @Test
    void testUnsupported() {
        RoundRobinBlockingQueue<Integer> queue = new RoundRobinBlockingQueue<>();
        assertThrows(UnsupportedOperationException.class, queue::iterator);
        assertThrows(UnsupportedOperationException.class, () -> queue.offer(null, 10, TimeUnit.SECONDS));
        assertThrows(UnsupportedOperationException.class, () -> queue.containsAll(null));
        assertThrows(UnsupportedOperationException.class, () -> queue.addAll(null));
        assertThrows(UnsupportedOperationException.class, () -> queue.removeAll(null));
        assertThrows(UnsupportedOperationException.class, () -> queue.retainAll(null));
        assertThrows(UnsupportedOperationException.class, () -> queue.toArray(new Integer[]{1, 2}));
        assertThrows(UnsupportedOperationException.class, () -> queue.drainTo(null, 0));
        assertThrows(UnsupportedOperationException.class, queue::toArray);
        assertEquals(Integer.MAX_VALUE, queue.remainingCapacity());
    }

    @Test
    void add() {
        RoundRobinBlockingQueue<Object> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        assertTrue(queue.isEmpty());
        Object future1 = new Object();
        queue.add(future1);
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());
        assertEquals(future1, queue.poll());
        assertEquals(0, queue.size());
        Object future2 = new Object();
        assertFalse(queue.contains(future2));
        queue.add(future1);
        queue.add(future2);
        assertFalse(queue.contains(null));
        assertTrue(queue.contains(future2));
        assertEquals(2, queue.size());
        queue.remove(future2);
        assertEquals(1, queue.size());
        assertFalse(queue.remove(future2));
        queue.clear();
        assertEquals(0, queue.size());
        queue.clear();
        assertEquals(0, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void drainTo() {
        RoundRobinBlockingQueue<Object> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Object future1 = new Object();
        Object future2 = new Object();
        queue.add(future1);
        queue.add(future2);
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        String contract2 = "0002";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Object future3 = new Object();
        Object future4 = new Object();
        Object future5 = new Object();
        queue.add(future3);
        queue.add(future4);
        queue.add(future5);

        List<Object> returnedVal = new ArrayList<>();
        int count = queue.drainTo(returnedVal);
        assertEquals(5, count);
        assertEquals(5, returnedVal.size());
        assertEquals(future1, returnedVal.get(0));
        assertEquals(future2, returnedVal.get(1));
        assertEquals(future3, returnedVal.get(2));
        assertEquals(future4, returnedVal.get(3));
        assertEquals(future5, returnedVal.get(4));
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void remove() {
        RoundRobinBlockingQueue<Object> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        assertThrows(NoSuchElementException.class, () -> queue.remove());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Object future1 = new Object();
        Object future2 = new Object();
        queue.add(future1);
        queue.add(future2);
        assertTrue(queue.remove(future2));
        assertEquals(1, queue.size());
        assertEquals(future1, queue.remove());
        assertEquals(0, queue.size());
        Object future3 = new Object();
        queue.add(future3);
        assertEquals(1, queue.size());
        assertTrue(queue.remove(future3));
        assertEquals(0, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void testOffer() throws InterruptedException {
        RoundRobinBlockingQueue<Object> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Object future1 = new Object();
        Object future2 = new Object();
        queue.add(future1);
        queue.add(future2);
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        String contract2 = "0002";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Object future3 = new Object();
        Object future4 = new Object();
        Object future5 = new Object();
        queue.add(future3);
        queue.add(future4);
        queue.add(future5);
        assertEquals(5, queue.size());
        assertEquals(future1, queue.take());
        assertEquals(future3, queue.take());
        assertEquals(future2, queue.take());
        assertEquals(future4, queue.take());
        assertEquals(future5, queue.take());
        assertEquals(0, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void peek() throws InterruptedException {
        RoundRobinBlockingQueue<Object> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Object future1 = new Object();
        Object future2 = new Object();
        queue.put(future1);
        queue.put(future2);
        assertEquals(2, queue.size());
        assertEquals(future1, queue.peek());
        assertEquals(2, queue.size());
        assertEquals(future1, queue.poll(2, TimeUnit.SECONDS));
        assertEquals(future2, queue.poll(2, TimeUnit.SECONDS));
        assertNull(queue.peek());
        assertThrows(NoSuchElementException.class, () -> queue.element());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void peekAgain() {
        RoundRobinBlockingQueue<Object> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        String contract2 = "0002";

        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Object future1 = new Object();
        Object future2 = new Object();
        queue.add(future1);
        queue.add(future2);
        assertEquals(2, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();

        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Object future3 = new Object();
        Object future4 = new Object();
        queue.add(future3);
        queue.add(future4);
        queue.remove();
        queue.remove();
        assertEquals(future2, queue.peek());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }
}
