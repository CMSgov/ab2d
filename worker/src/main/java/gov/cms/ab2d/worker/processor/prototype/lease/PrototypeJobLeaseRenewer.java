package gov.cms.ab2d.worker.processor.prototype.lease;

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
 * <p>
 * TODO:This creates a rare case where the worker is dead but appears alive if no work is happening but this
 *      scheduled heartbeat keeps going off on schedule.
 *      Ideally we just implement a backoff such that no progress on a job for long enough
 *      naturally leads to expiry of the token.
 */
@Slf4j
@Component
public class PrototypeJobLeaseRenewer  {

	private final JobLeaseRepository jobLease;
	private final PrototypeProperties props;
	// map of (jobId, fenceToken) to heartbeat context
	private final Map<PrototypeJobLeaseToken, HeartbeatContext> activeTokens = new ConcurrentHashMap<>();

	public PrototypeJobLeaseRenewer(JobLeaseRepository jobLease, PrototypeProperties props) {
		this.jobLease = jobLease;
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
				if (LocalDateTime.now().isAfter(context.getMaxLatestHeartbeat())) {
					log.warn("Too much time elapsed since last heartbeat - not renewing. Last heartbeat: {}, Last event: {}",
						context.getLastHeartbeatAt(),
						context.getLastEvent()
					);
					activeTokens.remove(entry.getKey());
				} else if (jobLease.renewHeartbeat(jobUuid, token)) {
					log.debug("renewed lease heartbeat for {} at token {}", jobUuid, token);
				} else {
					// we lost the lease, remove it from our active jobs
					log.warn("lease for {} no longer held at token {}", jobUuid, token);
					activeTokens.remove(entry.getKey());
				}
			} catch (Exception e) {
				log.warn("error renewing lease heartbeat for {} at token {}", jobUuid, token, e);
			}
		}
	}

	public void postHeartbeat(String jobUuid, long fenceToken, HeartbeatEvent event) {
		val now = LocalDateTime.now();
		final LocalDateTime maxLatestHeartBeat;
		switch (event) {
			case CREATE_LEASE -> {
				maxLatestHeartBeat = now.plusSeconds(props.getMaxDurationSecondsAfterCreateLease());
			}
			case BEFORE_CREATE_AGGREGATED_TABLE -> {
				maxLatestHeartBeat = now.plusSeconds(props.getMaxDurationSecondsCreateAggregatedTable());
			}
			case AFTER_WRITE_CALLBACK -> {
				maxLatestHeartBeat = now.plusSeconds(props.getMaxDurationSecondsAfterWriteCallback());
			}
			case BEFORE_ASSEMBLE_FILES -> {
				maxLatestHeartBeat = now.plusSeconds(props.getMaxDurationSecondsAssembleFiles());
			}
			default -> throw new IllegalStateException("Invalid value: " + event);
		}

		activeTokens.put(new PrototypeJobLeaseToken(jobUuid, fenceToken), new HeartbeatContext(now, event, maxLatestHeartBeat));
	}

}
