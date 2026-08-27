package gov.cms.ab2d.worker.processor.prototype.lease.heartbeat;

public enum HeartbeatEvent {
	CREATE_LEASE,
	BEFORE_CREATE_AGGREGATED_TABLE,
	AFTER_WRITE_CALLBACK,
	BEFORE_ASSEMBLE_FILES;
}
