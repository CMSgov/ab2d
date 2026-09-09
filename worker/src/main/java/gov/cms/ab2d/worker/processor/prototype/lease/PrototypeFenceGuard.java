package gov.cms.ab2d.worker.processor.prototype.lease;

import gov.cms.ab2d.worker.processor.prototype.PrototypeMetrics;
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
    private final PrototypeMetrics metrics;
    private final PrototypeJobLeaseRenewer leaseRenewer;
    private final String jobUuid;
    private final long token;

    public PrototypeFenceGuard(
            JobLeaseRepository jobLease,
            PrototypeMetrics metrics,
            PrototypeJobLeaseRenewer leaseRenewer,
            String jobUuid,
            long token) {
        this.jobLease = jobLease;
        this.metrics = metrics;
	    this.leaseRenewer = leaseRenewer;
	    this.jobUuid = jobUuid;
        this.token = token;
    }

    @Override
    public void afterWrite(@NonNull Chunk<?> items) {
        try {
            jobLease.assertHoldsAndBeat(jobUuid, token);
        } catch (FenceLostException e) {
            // Record before rethrowing: the exception rolls the chunk back and unwinds the step, so this is
            // the only place that knows a commit was refused because ownership moved.
            log.warn("job {} lost the fence at token {} while committing a chunk", jobUuid, token);
            metrics.chunkFenceLost();
            log.warn("Untracking lease token ({}, {}) due to FenceLostException", jobUuid, token);
            leaseRenewer.untrack(jobUuid, token);
            throw e;
        }
        leaseRenewer.postHeartbeat(jobUuid, token, AFTER_WRITE_CALLBACK);
    }
}
