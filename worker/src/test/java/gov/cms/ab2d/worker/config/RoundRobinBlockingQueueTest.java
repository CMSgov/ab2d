package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.StreamHelper;
import gov.cms.ab2d.worker.processor.TextStreamHelperImpl;
import gov.cms.ab2d.worker.processor.stub.PatientClaimsProcessorSimple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class RoundRobinBlockingQueueTest {
    @Autowired
    private WorkerConfig workerConfig;
    private PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorSimple();
    private Map<String, StreamHelper> helpers = new HashMap<>();
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void init() {
        ((PatientClaimsProcessorSimple) patientClaimsProcessor).setExecutor(workerConfig.patientProcessorThreadPool());
    }

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
        assertThrows(UnsupportedOperationException.class, queue::toArray);
        assertEquals(Integer.MAX_VALUE, queue.remainingCapacity());
    }

    @Test
    void add() throws FileNotFoundException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        assertTrue(queue.isEmpty());
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        queue.add(future1);
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());
        assertEquals(future1, queue.poll());
        assertEquals(0, queue.size());
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        assertFalse(queue.contains(future2));
        queue.add(future1);
        queue.add(future2);
        System.out.println(queue.toString());
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
    void drainTo() throws FileNotFoundException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        queue.add(future1);
        queue.add(future2);
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        String contract2 = "0002";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Future<Void> future3 = patientClaimsProcessor.process(getNewPatient("3"), getHelper(contract2), null);
        Future<Void> future4 = patientClaimsProcessor.process(getNewPatient("4"), getHelper(contract2), null);
        Future<Void> future5 = patientClaimsProcessor.process(getNewPatient("5"), getHelper(contract2), null);
        queue.add(future3);
        queue.add(future4);
        queue.add(future5);
        List<Future<Void>> returnedVal = new ArrayList<>();
        queue.drainTo(returnedVal);
        assertNotNull(returnedVal);
        assertEquals(5, returnedVal.size());
        assertEquals(future1, returnedVal.get(0));
        assertEquals(future3, returnedVal.get(1));
        assertEquals(future2, returnedVal.get(2));
        assertEquals(future4, returnedVal.get(3));
        assertEquals(future5, returnedVal.get(4));
        assertEquals(0, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void testDrainTo() throws FileNotFoundException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        queue.add(future1);
        queue.add(future2);
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        String contract2 = "0002";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Future<Void> future3 = patientClaimsProcessor.process(getNewPatient("3"), getHelper(contract2), null);
        Future<Void> future4 = patientClaimsProcessor.process(getNewPatient("4"), getHelper(contract2), null);
        Future<Void> future5 = patientClaimsProcessor.process(getNewPatient("5"), getHelper(contract2), null);
        queue.add(future3);
        queue.add(future4);
        queue.add(future5);
        List<Future<Void>> returnedVal = new ArrayList<>();
        queue.drainTo(returnedVal, 3);
        assertNotNull(returnedVal);
        assertEquals(3, returnedVal.size());
        assertEquals(future1, returnedVal.get(0));
        assertEquals(future3, returnedVal.get(1));
        assertEquals(future2, returnedVal.get(2));
        assertEquals(2, queue.size());
        queue.drainTo(returnedVal, 99);
        assertEquals(0, queue.size());
        queue.drainTo(returnedVal, 5);
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void remove() throws FileNotFoundException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        assertThrows(NoSuchElementException.class, () -> queue.remove());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        queue.add(future1);
        queue.add(future2);
        assertTrue(queue.remove(future2));
        assertEquals(1, queue.size());
        assertEquals(future1, queue.remove());
        assertEquals(0, queue.size());
        Future<Void> future3 = patientClaimsProcessor.process(getNewPatient("3"), getHelper(contract1), null);
        queue.add(future3);
        assertEquals(1, queue.size());
        assertTrue(queue.remove(future3));
        assertEquals(0, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void testOffer() throws FileNotFoundException, InterruptedException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        queue.add(future1);
        queue.add(future2);
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        String contract2 = "0002";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Future<Void> future3 = patientClaimsProcessor.process(getNewPatient("3"), getHelper(contract2), null);
        Future<Void> future4 = patientClaimsProcessor.process(getNewPatient("4"), getHelper(contract2), null);
        Future<Void> future5 = patientClaimsProcessor.process(getNewPatient("5"), getHelper(contract2), null);
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
    void peek() throws FileNotFoundException, InterruptedException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        queue.put(future1);
        queue.put(future2);
        assertEquals(2, queue.size());
        assertEquals(future1, queue.peek());
        assertEquals(2, queue.size());
        assertEquals(future1, queue.poll(2, TimeUnit.SECONDS));
        assertEquals(future2, queue.poll(2, TimeUnit.SECONDS));
        assertNull(queue.element());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    @Test
    void peekAgain() throws FileNotFoundException, InterruptedException {
        RoundRobinBlockingQueue<Future<Void>> queue = new RoundRobinBlockingQueue<>();
        String contract1 = "0001";
        String contract2 = "0002";

        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract1);
        Future<Void> future1 = patientClaimsProcessor.process(getNewPatient("1"), getHelper(contract1), null);
        Future<Void> future2 = patientClaimsProcessor.process(getNewPatient("2"), getHelper(contract1), null);
        queue.add(future1);
        queue.add(future2);
        assertEquals(2, queue.size());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();

        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(contract2);
        Future<Void> future3 = patientClaimsProcessor.process(getNewPatient("3"), getHelper(contract1), null);
        Future<Void> future4 = patientClaimsProcessor.process(getNewPatient("4"), getHelper(contract1), null);
        queue.add(future3);
        queue.add(future4);
        queue.remove();
        queue.remove();
        assertEquals(future2, queue.peek());
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
    }

    private GetPatientsByContractResponse.PatientDTO getNewPatient(String patientId) {
        GetPatientsByContractResponse.PatientDTO dto = new GetPatientsByContractResponse.PatientDTO();
        dto.setPatientId(patientId);
        return dto;
    }

    private StreamHelper getHelper(String contract) throws FileNotFoundException {
        StreamHelper helper = helpers.get(contract);
        if (helper == null) {
            helper = new TextStreamHelperImpl(Path.of("/tmp"), contract, 1000, 60);
            helpers.put(contract, helper);
        }
        return helper;
    }
}