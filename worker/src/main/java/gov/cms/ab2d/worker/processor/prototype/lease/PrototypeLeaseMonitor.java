package gov.cms.ab2d.worker.processor.prototype.lease;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.worker.processor.prototype.PrototypeMetrics;
import gov.cms.ab2d.worker.processor.prototype.PrototypeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_PROTOTYPE_ENABLED;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;

/**
 * Watches the lease table so that recovery itself is observable rather than assumed.
 *
 * An IN_PROGRESS job whose heartbeat is older than the TTL is normal for a short window: the owner died and
 * the next poll will take the job over. If the same job is still sitting there well past the TTL, nothing
 * picked it up and the job is stranded - no worker is advancing it and no worker will fail it either. That
 * is the failure mode this monitor exists to catch, so it gauges both counts and alerts on the second.
 *
 * Runs on every worker. The gauges are per-instance snapshots of the same shared table, so read them in
 * Datadog with max/avg by host rather than as a sum.
 *
 * Alerting is deliberately not per-poll. A stranded job stays stranded, so N workers polling every minute
 * would otherwise emit N alerts a minute for hours. Instead each alert is claimed atomically in the database
 * and stamped with a cooldown, which both dedupes across workers and rate-limits over time: one message per
 * stranded job per cooldown, no matter how many workers are running.
 */
@Slf4j
@Component
public class PrototypeLeaseMonitor {

    private final JobLeaseRepository jobLease;
    private final PrototypeMetrics metrics;
    private final PropertiesService propertiesService;
    private final SQSEventClient eventLogger;
    private final PrototypeProperties props;
    private final int leaseTtlSeconds;

    public PrototypeLeaseMonitor(JobLeaseRepository jobLease, PrototypeMetrics metrics,
                                 PropertiesService propertiesService, SQSEventClient eventLogger,
                                 PrototypeProperties props,
                                 @Value("${job.lock.ttl}") int leaseTtlSeconds) {
        this.jobLease = jobLease;
        this.metrics = metrics;
        this.propertiesService = propertiesService;
        this.eventLogger = eventLogger;
        this.props = props;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${pause-resume.prototype.lease-monitor-ms:60000}")
    public void reportLeaseHealth() {
        if (!propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)) {
            return;
        }

        int graceSeconds = graceSeconds();
        try {
            JobLeaseRepository.HeartbeatHealth health = jobLease.heartbeatHealth(leaseTtlSeconds, graceSeconds);
            metrics.leaseHeartbeats(health.active(), health.stale(), health.unrecovered());

            if (health.unrecovered() > 0) {
                alertUnrecovered(graceSeconds);
            }
        } catch (Exception e) {
            // Monitoring must never take the worker down with it.
            log.error("prototype lease monitor failed to read heartbeat health", e);
        }
    }

    /**
     * Name the stranded jobs so an on-call engineer can go straight to them. Sent as a trace rather than a
     * full alert: it is an AB2D-team operational signal, not a customer-visible job outcome.
     *
     * Only the jobs this worker wins the claim for are reported. Losing the claim is the normal case when
     * several workers poll at once, and means another worker has already said it.
     */
    private void alertUnrecovered(int graceSeconds) {
        List<String> uuids = jobLease.claimUnrecoveredForAlert(graceSeconds, cooldownSeconds());
        if (uuids.isEmpty()) {
            log.debug("stranded jobs present but already alerted within the cooldown; staying quiet");
            return;
        }
        String message = String.format(
                "AB2D pause/resume: %d IN_PROGRESS job(s) have had a dead lease for more than %ds "
                        + "(lease TTL %ds) and have not been recovered by any worker: %s",
                uuids.size(), graceSeconds, leaseTtlSeconds, uuids);
        log.error(message);
        eventLogger.trace(message, PUBLIC_LIST);
    }

    /** How long a stranded job stays quiet after being reported, so an incident does not flood Slack. */
    private int cooldownSeconds() {
        return props.getLeaseAlertCooldownMinutes() * 60;
    }

    /**
     * How long past the TTL a dead lease has to sit before we call recovery broken. A multiple of the TTL so
     * the two stay in step if the TTL is retuned.
     */
    private int graceSeconds() {
        return Math.max(leaseTtlSeconds, leaseTtlSeconds * props.getLeaseGraceMultiplier());
    }
}
