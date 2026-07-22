package gov.cms.ab2d.worker.processor.prototype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renews the lock for tracked jobs, maintaining the lock for the worker while it processes.
 * We renew against the repository directly rather than via the registry because JdbcLock.renew() requires the
 * calling thread to hold the lock, and this scheduled thread is not the one processing the job.
 */
@Slf4j
@Component
public class PrototypeJobLockRenewer {

    private final LockRepository lockRepository;
    private final Set<String> activeJobUuids = ConcurrentHashMap.newKeySet();

    public PrototypeJobLockRenewer(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    public void track(String jobUuid) {
        activeJobUuids.add(jobUuid);
    }

    public void untrack(String jobUuid) {
        activeJobUuids.remove(jobUuid);
    }

    /**
     * Renew every active job's lock. The renewal interval should be significantly shorter than the
     * lock's TTL
     */
    @Scheduled(fixedDelayString = "${pause-resume.prototype.lock-renew-ms:5000}")
    public void renewActiveLocks() {
        for (String jobUuid : activeJobUuids) {
            String lockKey = UUIDConverter.getUUID(jobUuid).toString();
            try {
                Random r = new Random();
                if (r.nextInt() % 10 == 0) {
                    System.out.println("Random event delays renewal.");
                    Thread.sleep(65000);
                }
                boolean renewed = lockRepository.renew(lockKey);
                if (renewed) {
                    log.debug("renewed job lock for {}", jobUuid);
                } else {
                    log.warn("failed to renew job lock for {} - lock may have expired or been reclaimed", jobUuid);
                }
            } catch (Exception e) {
                log.warn("error renewing job lock for {}", jobUuid, e);
            }
        }
    }
}
