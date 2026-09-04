package gov.cms.ab2d.worker.processor.prototype.lease;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.infrastructure.item.Chunk;

import static gov.cms.ab2d.worker.processor.prototype.lease.heartbeat.HeartbeatEvent.AFTER_WRITE_CALLBACK;

/**
 * Registers on the worker step. Before committing a chunk, check the token to ensure
 * we still hold the lease. If we don't, the chunk will roll back.
 */
@Slf4j
public class PrototypeFenceGuard implements ItemWriteListener<Object> {

    private final JobLeaseRepository jobLease;
    private final PrototypeJobLeaseRenewer leaseRenewer;
    private final String jobUuid;
    private final long token;

    public PrototypeFenceGuard(JobLeaseRepository jobLease, PrototypeJobLeaseRenewer leaseRenewer, String jobUuid, long token) {
        this.jobLease = jobLease;
	    this.leaseRenewer = leaseRenewer;
	    this.jobUuid = jobUuid;
        this.token = token;
    }

    @Override
    public void afterWrite(@NonNull Chunk<?> items) {
        try {
            jobLease.assertHoldsAndBeat(jobUuid, token);
            leaseRenewer.postHeartbeat(jobUuid, token, AFTER_WRITE_CALLBACK);
        } catch (FenceLostException e) {
            log.info("Untracking lease token ({}, {}) due to FenceLostException", jobUuid, token);
            leaseRenewer.untrack(jobUuid, token);
            throw e;
        }
    }
}
