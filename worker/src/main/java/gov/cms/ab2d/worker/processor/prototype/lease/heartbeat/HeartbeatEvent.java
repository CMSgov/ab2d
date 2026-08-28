package gov.cms.ab2d.worker.processor.prototype.lease.heartbeat;

public enum HeartbeatEvent {
	// When job lease is first created
	CREATE_LEASE,
	// Before aggregated table is created
	BEFORE_CREATE_AGGREGATED_TABLE,
	// When ItemWriteListener#afterWrite callback is invoked
	AFTER_WRITE_CALLBACK,
	// Before files are assembled prior to completing a job
	BEFORE_ASSEMBLE_FILES;
}
