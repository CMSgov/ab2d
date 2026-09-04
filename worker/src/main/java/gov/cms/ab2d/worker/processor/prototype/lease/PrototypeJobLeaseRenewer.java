package gov.cms.ab2d.worker.processor.prototype.lease;

import gov.cms.ab2d.worker.processor.prototype.PrototypeMetrics;
import gov.cms.ab2d.worker.processor.prototype.PrototypeProperties;
import gov.cms.ab2d.worker.processor.prototype.lease.heartbeat.HeartbeatContext;
import gov.cms.ab2d.worker.processor.prototype.lease.heartbeat.HeartbeatEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gov.cms.ab2d.worker.processor.prototype.lease.heartbeat.HeartbeatEvent.CREATE_LEASE;

/**
 * Renews the heartbeat for jobs this worker is working on periodically.
 * This prevents long chunk commit times from accidentally triggering the loss of a lease.
 *
 * The job lease token will be renewed periodically as long as an in-memory heartbeat is posted.
 * If the deadline for a heartbeat is missed, it is assumed the worker could be dead in which case the token will
 * be removed and lease will be lost.
 *
 */
@Slf4j
@Component
public class PrototypeJobLeaseRenewer {

    private final JobLeaseRepository jobLease;
    private final PrototypeMetrics metrics;
	private final PrototypeProperties props;
	// map of (jobId, fenceToken) to heartbeat context
	private final Map<PrototypeJobLeaseToken, HeartbeatContext> activeTokens = new ConcurrentHashMap<>();

    public PrototypeJobLeaseRenewer(JobLeaseRepository jobLease, PrototypeMetrics metrics, PrototypeProperties props) {
        this.jobLease = jobLease;
        this.metrics = metrics;
	    this.props = props;
    }

    public void track(String jobUuid, long fenceToken) {
	    postHeartbeat(jobUuid, fenceToken, CREATE_LEASE);
    }

    public void untrack(String jobUuid, long fenceToken) {
	    activeTokens.remove(new PrototypeJobLeaseToken(jobUuid, fenceToken));
    }

    @Scheduled(fixedDelayString = "${pause-resume.prototype.lease-renew-ms:20000}")
    public void renewActiveLeases() {
	    for (Map.Entry<PrototypeJobLeaseToken, HeartbeatContext> entry : activeTokens.entrySet()) {
		    val jobUuid = entry.getKey().jobUuid();
		    long token = entry.getKey().fenceToken();
		    val context = entry.getValue();
		    try {
				// Missed deadline for heartbeat; Current operation is taking longer than expected or worker may be dead
			    if (LocalDateTime.now().isAfter(context.maxLatestNextHeartbeat())) {
				    log.warn("Too much time elapsed since last heartbeat - not renewing. Last heartbeat: {}; Last event: {}",
					    context.lastHeartbeatAt(),
					    context.event()
				    );
				    activeTokens.remove(entry.getKey());
			    } else if (jobLease.renewHeartbeat(jobUuid, token)) {
				    log.debug("renewed lease heartbeat for {} at token {}", jobUuid, token);
			    } else {
				    // we lost the lease, remove it from our active jobs
				    log.warn("lease for {} no longer held at token {}", jobUuid, token);
				    metrics.leaseRenewFailed();
				    activeTokens.remove(entry.getKey());
			    }
		    } catch (Exception e) {
			    log.warn("error renewing lease heartbeat for {} at token {}", jobUuid, token, e);
		    }
	    }
    }

	/**
	 * Post in-memory heartbeat and set deadline for next expected heartbeat based on event
	 */
	public void postHeartbeat(String jobUuid, long fenceToken, HeartbeatEvent event) {
		val now = LocalDateTime.now();
		final LocalDateTime maxLatestNextHeartBeat;
		switch (event) {
			case CREATE_LEASE ->
					maxLatestNextHeartBeat = now.plusSeconds(props.getMaxDurationSecondsCreateLease());
			case CREATE_AGGREGATED_TABLE ->
					maxLatestNextHeartBeat = now.plusSeconds(props.getMaxDurationSecondsCreateAggregatedTable());
			case AFTER_WRITE_CALLBACK ->
					maxLatestNextHeartBeat = now.plusSeconds(props.getMaxDurationSecondsPerItem() * props.getChunkSize());
			case ASSEMBLE_FILES ->
					maxLatestNextHeartBeat = now.plusSeconds(props.getMaxDurationSecondsAssembleFiles());
			default -> throw new IllegalStateException("Invalid value: " + event);
		}

		val heartbeatContext = new HeartbeatContext(now, event, maxLatestNextHeartBeat);
		val leaseToken = new PrototypeJobLeaseToken(jobUuid, fenceToken);
		if (event == CREATE_LEASE) {
			activeTokens.put(leaseToken, heartbeatContext);
			log.debug("Creating new lease and posting heartbeat: {}", heartbeatContext);
		} else if (activeTokens.replace(leaseToken, heartbeatContext) != null) {
			log.debug("Posting heartbeat: {}", heartbeatContext);
		} else {
			log.warn("Posting heartbeat failed - lease token was removed");
		}
	}
}
