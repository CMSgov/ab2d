package gov.cms.ab2d.worker.processor.pauseresume.lease.heartbeat;

import gov.cms.ab2d.worker.processor.pauseresume.lease.PauseResumeJobLeaseRenewer;

import java.time.LocalDateTime;

/**
 * In-memory heartbeat used to determine whether {@link PauseResumeJobLeaseRenewer} should continue renewing the lease
 * on a regular interval.
 */
public record HeartbeatContext(
	LocalDateTime lastHeartbeatAt,
	HeartbeatEvent event,
	LocalDateTime maxLatestNextHeartbeat,
	int chunkSize) {
}
