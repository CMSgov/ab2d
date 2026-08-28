package gov.cms.ab2d.worker.processor.prototype.lease.heartbeat;

import java.time.LocalDateTime;

public record HeartbeatContext(
	LocalDateTime lastHeartbeatAt,
	HeartbeatEvent lastEvent,
	LocalDateTime maxLatestNextHeartbeat) {
}
