package gov.cms.ab2d.worker.processor.prototype.lease.heartbeat;

import gov.cms.ab2d.worker.processor.prototype.lease.PrototypeFenceGuard;

public enum HeartbeatEvent {
	/** When job lease is first created */
	CREATE_LEASE,
	/** When aggregated table is created */
	CREATE_AGGREGATED_TABLE,
	/** When {@link PrototypeFenceGuard#afterWrite} callback is invoked */
	AFTER_WRITE_CALLBACK,
	/** When files are assembled prior to completing a job */
	ASSEMBLE_FILES;
}
