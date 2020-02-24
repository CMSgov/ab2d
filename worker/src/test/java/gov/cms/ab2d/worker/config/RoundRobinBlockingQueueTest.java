package gov.cms.ab2d.worker.config;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinBlockingQueueTest {
    @Test
    void testAdditions() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        assertEquals(0, queue.size());
        assertTrue(queue.add("One", "a"));
        assertEquals(1, queue.size());
        assertTrue(queue.add("One", "b"));
        assertEquals(2, queue.size());
        queue.add("Two", "c");
        assertEquals(3, queue.size());
        queue.add("Two", "d");
        assertEquals(4, queue.size());
        queue.add("Three", "e");
        assertEquals(5, queue.size());
        System.out.println(queue.toString());
        assertEquals("a", queue.element());
        assertEquals("a", queue.poll());
        assertEquals("c", queue.peek());
        assertEquals("c", queue.poll());
        queue.add("Four", "f");
        assertEquals("e", queue.peek());
        assertEquals("e", queue.poll());
        assertEquals("f", queue.peek());
        assertEquals("f", queue.poll());
        assertEquals("b", queue.peek());
        assertEquals("b", queue.poll());
        assertEquals("d", queue.peek());
        assertEquals("d", queue.poll());
        System.out.println(queue.toString());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testRemovals() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        assertNull(queue.peek());
        queue.add("One", "a");
        queue.add("One", "b");

        queue.add("Two", "c");
        queue.add("Two", "d");

        queue.add("Three", "e");

        queue.remove("c");
        assertEquals("a", queue.poll());
        assertEquals("d", queue.peek());
        assertEquals("d", queue.poll());
        assertEquals("e", queue.poll());
        assertEquals("b", queue.poll());
        assertNull(queue.poll());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testExceptions() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        assertThrows(UnsupportedOperationException.class, () -> queue.add("Hello"));
        assertThrows(UnsupportedOperationException.class, () -> queue.put("Hello"));
        assertThrows(UnsupportedOperationException.class, () -> queue.iterator());
        assertThrows(UnsupportedOperationException.class, () -> queue.offer("Hello"));
        assertThrows(NoSuchElementException.class, () -> queue.remove());
        assertThrows(UnsupportedOperationException.class, () -> queue.offer("Hello", 1000, TimeUnit.MILLISECONDS));
        assertThrows(UnsupportedOperationException.class, () -> queue.put("Hello"));
        assertThrows(UnsupportedOperationException.class, () -> queue.addAll(new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, () -> queue.retainAll(new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, () -> queue.toArray(new String[4]));
        assertThrows(UnsupportedOperationException.class, () -> queue.toArray());
        assertThrows(UnsupportedOperationException.class, () -> queue.take());
        assertEquals(Integer.MAX_VALUE, queue.remainingCapacity());
    }

    @Test
    void testRemoveAll() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        assertTrue(queue.removeAll(new ArrayList<>()));
        queue.add("One", "a");
        queue.add("One", "b");

        queue.add("Two", "c");
        queue.add("Two", "d");

        queue.add("Three", "e");
        assertEquals(5, queue.size());
        assertFalse(queue.removeAll(Lists.newArrayList("x", "y")));
        assertEquals(5, queue.size());
        assertFalse(queue.removeAll(Lists.newArrayList("d", "y")));
        assertEquals(4, queue.size());
        assertTrue(queue.removeAll(Lists.newArrayList("c", "e")));
        assertEquals(2, queue.size());
        assertEquals("a", queue.poll());
        assertEquals("b", queue.poll());
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testContains() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        queue.add("One", "a");
        queue.add("One", "b");
        assertTrue(queue.contains("a"));
        assertTrue(queue.containsAll(Lists.newArrayList("a", "b")));
        assertFalse(queue.containsAll(Lists.newArrayList("a", "x")));
        assertFalse(queue.contains("x"));
        assertFalse(queue.contains(null));
        assertEquals(2, queue.size());
        queue.clear();
        assertEquals(0, queue.size());
        assertNull(queue.poll());
    }

    @Test
    void testOtherAds() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        assertEquals(0, queue.size());
        assertTrue(queue.addAll("One", Collections.emptyList()));
        assertEquals(0, queue.size());
        assertTrue(queue.addAll("One", Lists.newArrayList("a", "b", "c")));
        assertEquals(3, queue.size());
        queue.addAll("Two", Lists.newArrayList("d", "e", "f"));
        assertEquals(6, queue.size());
        assertEquals("a", queue.poll());
        assertEquals(5, queue.size());
        assertEquals("d", queue.poll());
        assertEquals(4, queue.size());
        assertEquals("b", queue.poll());
        assertEquals(3, queue.size());
        assertEquals("e", queue.poll());
        assertEquals(2, queue.size());
        assertEquals("c", queue.poll());
        assertEquals(1, queue.size());
        assertEquals("f", queue.remove());
        assertEquals(0, queue.size());
        assertNull(queue.poll());
    }

    @Test
    void testDrainTo() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        queue.addAll("One", Lists.newArrayList("a", "b", "c"));
        queue.addAll("Two", Lists.newArrayList("d", "e", "f"));
        List<String> items = new ArrayList<>();
        assertEquals(6, queue.drainTo(items));
        assertEquals(6, items.size());
        assertEquals(0, queue.size());
        assertEquals("a", items.get(0));
        assertEquals("d", items.get(1));
        assertEquals("b", items.get(2));
        assertEquals("e", items.get(3));
        assertEquals("c", items.get(4));
        assertEquals("f", items.get(5));
        assertNull(queue.poll());

    }

    @Test
    void testDrainToAgain() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        queue.addAll("One", Lists.newArrayList("a", "b", "c"));
        queue.addAll("Two", Lists.newArrayList("d", "e", "f"));
        List<String> items = new ArrayList<>();
        assertEquals(3, queue.drainTo(items, 3));
        assertEquals(3, items.size());
        assertEquals(3, queue.size());
        assertEquals("e", queue.poll());
        assertEquals("c", queue.poll());
        assertEquals("f", queue.poll());

    }

    @Test
    void testDrainToAgainAgain() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        queue.addAll("One", Lists.newArrayList("a", "b", "c"));
        queue.addAll("Two", Lists.newArrayList("d", "e", "f"));
        List<String> items = new ArrayList<>();
        assertEquals(6, queue.drainTo(items, 99));
        assertEquals(6, items.size());
        assertEquals(0, queue.size());
        assertEquals("a", items.get(0));

    }

    @Test
    void testDrainToAgainAgainAndAgain() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        queue.addAll("One", Lists.newArrayList("a", "b", "c"));
        queue.addAll("Two", Lists.newArrayList("d", "e", "f"));
        List<String> items = new ArrayList<>();
        assertEquals(6, queue.drainTo(items, 6));
        assertEquals(6, items.size());
        assertEquals(0, queue.size());
        assertEquals("a", items.get(0));

    }

    @Test
    void testPollTimeout() {
        RoundRobinBlockingQueue<String> queue = new RoundRobinBlockingQueue<>();
        queue.addAll("One", Lists.newArrayList("a", "b", "c"));
        try {
            assertEquals("a", queue.poll(1, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            fail(ex);
        }
    }

    public static boolean testIfEven(Integer it) {
        if (it == null) {
            return false;
        }
        return it % 2 == 0;
    }

    @Test
    void testRemoveIfTrue() {
        RoundRobinBlockingQueue<Integer> queue = new RoundRobinBlockingQueue<>();
        queue.addAll("One", Lists.newArrayList(1, 2, 3, 4, 5));
        queue.addAll("Two", Lists.newArrayList(6, 7, 8, 9, 10));
        queue.removeIfTrue(RoundRobinBlockingQueueTest::testIfEven);
        assertEquals(5, queue.size());
        assertEquals(1, queue.poll());
        assertEquals(7, queue.poll());
        assertEquals(3, queue.poll());
        assertEquals(9, queue.poll());
        assertEquals(5, queue.poll());
        assertNull(queue.poll());
    }
}