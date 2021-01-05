package gov.cms.ab2d.worker.processor.coverage;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

/**
 * This is created so that multiple threads can attempt to secure a lock
 */
public class LockThread implements Callable<Boolean> {
    private final CoverageLockWrapper coverageLockWrapper;
    private final int id;

    public LockThread(CoverageLockWrapper coverageLockWrapper, int id) {
        this.coverageLockWrapper = coverageLockWrapper;
        this.id = id;
    }

    @Override
    public Boolean call() throws Exception {
        System.out.println("Thread " + id + " starting");
        Lock lock = coverageLockWrapper.getCoverageLock();
        boolean canGetIt = lock.tryLock();
        if (!canGetIt) {
            return false;
        }
        Thread.sleep(3000);
        lock.unlock();
        System.out.println("Thread " + id + " ending");
        return canGetIt;
    }
}
