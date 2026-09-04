package gov.cms.ab2d.worker.processor.prototype.lease.heartbeat;

import gov.cms.ab2d.worker.processor.prototype.lease.PrototypeJobLeaseRenewer;

import java.time.LocalDateTime;

/**
 * In-memory heartbeat used to determine whether {@link PrototypeJobLeaseRenewer} should continue renewing the lease
 * on a regular interval.
 */
public record HeartbeatContext(
	LocalDateTime lastHeartbeatAt,
	HeartbeatEvent event,
	LocalDateTime maxLatestNextHeartbeat) {
}
