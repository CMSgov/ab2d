package gov.cms.ab2d.worker.processor.prototype.lease;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.infrastructure.item.Chunk;

/**
 * Registers on the worker step. Before committing a chunk, check the token to ensure
 * we still hold the lease. If we don't, the chunk will roll back.
 */
@Slf4j
public class PrototypeFenceGuard implements ItemWriteListener<Object> {

    private final JobLeaseRepository jobLease;
    private final String jobUuid;
    private final long token;

    public PrototypeFenceGuard(JobLeaseRepository jobLease, String jobUuid, long token) {
        this.jobLease = jobLease;
        this.jobUuid = jobUuid;
        this.token = token;
    }

    @Override
    public void afterWrite(@NonNull Chunk<?> items) {
        jobLease.assertHoldsAndBeat(jobUuid, token);
    }
}
