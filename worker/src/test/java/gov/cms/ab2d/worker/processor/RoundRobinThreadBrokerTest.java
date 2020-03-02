package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.config.WorkerConfig;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class RoundRobinThreadBrokerTest {
    @Autowired
    private WorkerConfig workerConfig;

    private RoundRobinThreadBroker roundRobinThreadBroker;
    private PatientClaimsProcessor patientClaimsProcessor = new PatientClaimsProcessorSimple();
    private Map<String, StreamHelper> helpers = new HashMap<>();

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void init() {
        roundRobinThreadBroker = new RoundRobinThreadBroker(patientClaimsProcessor, workerConfig);
        ((PatientClaimsProcessorSimple) patientClaimsProcessor).setThreadPoolExecutor(
                workerConfig.patientProcessorThreadPool());
    }

    @Test
    void addToBroker() throws FileNotFoundException, ExecutionException, InterruptedException {
        String contract1 = "0001";
        String contract2 = "0002";
        ThreadRequest req1 = getThreadRequest(contract1, "1");
        ThreadRequest req2 = getThreadRequest(contract1, "2");
        ThreadRequest req3 = getThreadRequest(contract2, "3");
        roundRobinThreadBroker.add(contract1, req1);
        roundRobinThreadBroker.add(contract1, req2);
        roundRobinThreadBroker.add(contract2, req3);
        assertEquals(0, roundRobinThreadBroker.getNumberDone(contract1));
        Future<Void> submittedThread1 = roundRobinThreadBroker.addNextThread();
        Future<Void> submittedThread2 = roundRobinThreadBroker.addNextThread();
        Future<Void> submittedThread3 = roundRobinThreadBroker.addNextThread();
        assertFalse(submittedThread1.isDone());
        assertFalse(submittedThread2.isDone());
        assertFalse(submittedThread3.isDone());
        assertFalse(roundRobinThreadBroker.isContractDone(contract1));
        assertFalse(roundRobinThreadBroker.isContractDone(contract2));
        Thread.sleep(400);
        assertTrue(submittedThread1.isDone());
        assertTrue(submittedThread2.isDone());
        assertTrue(submittedThread3.isDone());
        assertTrue(roundRobinThreadBroker.isContractDone(contract1));
        assertTrue(roundRobinThreadBroker.isContractDone(contract2));
    }

    private GetPatientsByContractResponse.PatientDTO getNewPatient(String patientId) {
        GetPatientsByContractResponse.PatientDTO dto = new GetPatientsByContractResponse.PatientDTO();
        dto.setPatientId(patientId);
        return dto;
    }

    private ThreadRequest getThreadRequest(String contract, String patientId) throws FileNotFoundException {
        ThreadRequest req = new ThreadRequest(getNewPatient(patientId), getHelper(contract), null);
        return req;
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