package gov.cms.ab2d.worker.processor.prototype.lease;

import gov.cms.ab2d.worker.processor.prototype.PrototypeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renews the heartbeat for jobs this worker is working on periodically.
 * This prevents long chunk commit times from accidentally triggering the loss of a lease.
 *
 * TODO:This creates a rare case where the worker is dead but appears alive if no work is happening but this
 *      scheduled heartbeat keeps going off on schedule.
 *      Ideally we just implement a backoff such that no progress on a job for long enough
 *      naturally leads to expiry of the token.
 *
 */
@Slf4j
@Component
public class PrototypeJobLeaseRenewer {

    private final JobLeaseRepository jobLease;
    private final PrototypeMetrics metrics;
    // map of jobId to the token this worker holds for it
    private final Map<String, Long> activeTokens = new ConcurrentHashMap<>();

    public PrototypeJobLeaseRenewer(JobLeaseRepository jobLease, PrototypeMetrics metrics) {
        this.jobLease = jobLease;
        this.metrics = metrics;
    }

    public void track(String jobUuid, long token) {
        activeTokens.put(jobUuid, token);
    }

    public void untrack(String jobUuid) {
        activeTokens.remove(jobUuid);
    }

    @Scheduled(fixedDelayString = "${pause-resume.prototype.lease-renew-ms:20000}")
    public void renewActiveLeases() {
        for (Map.Entry<String, Long> entry : activeTokens.entrySet()) {
            String jobUuid = entry.getKey();
            long token = entry.getValue();
            try {
                if (jobLease.renewHeartbeat(jobUuid, token)) {
                    log.debug("renewed lease heartbeat for {} at token {}", jobUuid, token);
                } else {
                    // we lost the lease, remove it from our active jobs
                    log.warn("lease for {} no longer held at token {}", jobUuid, token);
                    metrics.leaseRenewFailed();
                    activeTokens.remove(jobUuid, token);
                }
            } catch (Exception e) {
                log.warn("error renewing lease heartbeat for {} at token {}", jobUuid, token, e);
            }
        }
    }
}
