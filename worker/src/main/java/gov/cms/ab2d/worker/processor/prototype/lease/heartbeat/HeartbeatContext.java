package gov.cms.ab2d.worker.processor.prototype.lease.heartbeat;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class HeartbeatContext {
	LocalDateTime lastHeartbeatAt;
	HeartbeatEvent lastEvent;
	LocalDateTime maxLatestNextHeartbeat;

	public HeartbeatContext(LocalDateTime lastHeartbeatAt, HeartbeatEvent lastEvent, LocalDateTime maxLatestHeartbeat) {
		this.lastHeartbeatAt = lastHeartbeatAt;
		this.lastEvent = lastEvent;
		this.maxLatestNextHeartbeat = maxLatestHeartbeat;
	}
}
