package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.config.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class RoundRobinThreadBroker {
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final WorkerConfig workerConfig;

    private List<String> contracts = new ArrayList<>();
    private Map<String, List<ThreadRequest>> threadRequests = new HashMap<>();
    private Map<String, List<Future<Void>>> futures = new HashMap<>();
    private Map<String, Integer> futuresDone = new HashMap<>();

    private int currentIndex = 0;

    @Value("${job.queue.multiplier}")
    private int multiplier;

    public RoundRobinThreadBroker(PatientClaimsProcessor patientClaimsProcessor, WorkerConfig workerConfig) {
        this.patientClaimsProcessor = patientClaimsProcessor;
        this.workerConfig = workerConfig;
    }

    public void updateQueue() {
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) workerConfig.patientProcessorThreadPool();
        int size = tpe.getMaximumPoolSize();
        BlockingQueue queue = tpe.getQueue();
        int numRemaining = (multiplier * size) - queue.size();
        if (numRemaining > 0) {
            for (int i = 0; i < numRemaining; i++) {
                addNextThread();
            }
        }
    }

    public synchronized void add(String contract, ThreadRequest request) {
        if (!contracts.contains(contract)) {
            contracts.add(contract);
        }
        List<ThreadRequest> requests = threadRequests.computeIfAbsent(contract, k -> new ArrayList<>());
        requests.add(request);
    }

    public synchronized Future<Void> pushThreadFromContract(String contract) {
        List<ThreadRequest> currentRequest = threadRequests.get(contract);
        ThreadRequest val = currentRequest.remove(0);
        if (currentRequest.size() == 0) {
            // No more requests so remove contract. We don't have to increment index since we removed the item at the
            // current index
            threadRequests.remove(contract);
        } else {
            currentIndex++;
        }
        log.info("Adding patient {} for contract {}", val.getPatientDTO().getPatientId(), contract);
        Future<Void> future = patientClaimsProcessor.process(val.getPatientDTO(), val.getHelper(), val.getAttTime());
        if (future != null) {
            List<Future<Void>> requests = futures.computeIfAbsent(contract, k -> new ArrayList<>());
            requests.add(future);
        }
        return future;
    }

    public synchronized Future<Void> addNextThread() {
        if (threadRequests.isEmpty()) {
            return null;
        }
        if (currentIndex >= threadRequests.size()) {
            currentIndex = 0;
        }
        String currentContract = contracts.get(currentIndex);
        return pushThreadFromContract(currentContract);
    }

    public synchronized boolean isContractDone(String contract) {
        List<ThreadRequest> th = threadRequests.get(contract);
        if (th != null && !threadRequests.isEmpty()) {
            return false;
        }
        List<Future<Void>> contractFutures = futures.get(contract);
        if (contractFutures == null || contractFutures.isEmpty()) {
            return true;
        }
        for (Future<Void> item : contractFutures) {
            if (!item.isDone()) {
                return false;
            }
        }
        return true;
    }

    public synchronized int getNumberDone(String contract) throws ExecutionException, InterruptedException {
        cleanUpThreads();
        Integer done = futuresDone.get(contract);
        if (done == null) {
            return 0;
        }
        return done;
    }

    public synchronized void cancelContract(final String contract) {
        List<ThreadRequest> th = threadRequests.get(contract);
        if (th != null) {
            th.clear();
        }
        List<Future<Void>> contractFutures = futures.get(contract);
        if (contractFutures == null) {
            return;
        }
        contractFutures.parallelStream().forEach(future -> {
            future.cancel(false);
            addOneDone(contract);
        });
        futures.remove(contract);
        threadRequests.remove(contract);
    }

    public synchronized void cleanUpThreads() {
        // Remove all done threads.
        for (Map.Entry<String, List<Future<Void>>> entry : futures.entrySet()) {
            List<Future<Void>> contractFutures = entry.getValue();
            for (Future<Void> f : contractFutures) {
                if (f.isDone() || f.isCancelled()) {
                    contractFutures.remove(f);
                    addOneDone(entry.getKey());
                }
            }
            // If all is done and there are no thread requests left, remove the contract
            List<ThreadRequest> contractRequests = threadRequests.get(entry.getKey());
            boolean noMoreRequests = !(contractRequests != null && contractRequests.size() > 0);
            boolean noMoreFutures = !(contractFutures != null && contractFutures.size() > 0);
            if (noMoreRequests) {
                threadRequests.remove(entry.getKey());
            }
            if (noMoreFutures) {
                futures.remove(entry.getKey());
            }
            if (noMoreFutures && noMoreRequests) {
                contracts.remove(entry.getKey());
            }
        }
    }

    private void addOneDone(String contract) {
        Integer val = futuresDone.get(contract);
        if (val == null) {
            futuresDone.put(contract, 1);
        } else {
            futuresDone.put(contract, val + 1);
        }
    }
}
