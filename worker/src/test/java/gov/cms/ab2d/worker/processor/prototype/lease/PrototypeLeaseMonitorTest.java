package gov.cms.ab2d.worker.processor.prototype.lease;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.worker.processor.prototype.PrototypeMetrics;
import gov.cms.ab2d.worker.processor.prototype.PrototypeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_PROTOTYPE_ENABLED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The lease monitor is what makes a stranded job visible, so these cover the three things that matter: it
 * stays out of the way when the prototype is off, it reports the heartbeat buckets when it is on, and it
 * names the stranded jobs when recovery has clearly stopped working.
 */
class PrototypeLeaseMonitorTest {

    private static final int TTL_SECONDS = 40;
    private static final int GRACE_MULTIPLIER = 3;
    private static final int GRACE_SECONDS = TTL_SECONDS * GRACE_MULTIPLIER;

    private JobLeaseRepository jobLease;
    private PrototypeMetrics metrics;
    private PropertiesService propertiesService;
    private SQSEventClient eventLogger;
    private PrototypeLeaseMonitor monitor;

    @BeforeEach
    void setUp() {
        jobLease = mock(JobLeaseRepository.class);
        metrics = mock(PrototypeMetrics.class);
        propertiesService = mock(PropertiesService.class);
        eventLogger = mock(SQSEventClient.class);

        PrototypeProperties props = new PrototypeProperties();
        props.setLeaseGraceMultiplier(GRACE_MULTIPLIER);
        monitor = new PrototypeLeaseMonitor(jobLease, metrics, propertiesService, eventLogger, props, TTL_SECONDS);
    }

    @Test
    @DisplayName("With the prototype turned off the monitor does not touch the database or emit anything")
    void disabledPrototypeIsSilent() {
        when(propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)).thenReturn(false);

        monitor.reportLeaseHealth();

        verifyNoInteractions(jobLease, metrics, eventLogger);
    }

    @Test
    @DisplayName("Stale leases are reported but not alerted, since a takeover is expected to happen")
    void staleLeasesAreReportedWithoutAlerting() {
        enabled();
        when(jobLease.heartbeatHealth(TTL_SECONDS, GRACE_SECONDS))
                .thenReturn(new JobLeaseRepository.HeartbeatHealth(2, 1, 0));

        monitor.reportLeaseHealth();

        verify(metrics).leaseHeartbeats(2, 1, 0);
        verify(eventLogger, never()).trace(anyString(), any());
        verify(jobLease, never()).unrecoveredJobUuids(anyInt());
    }

    @Test
    @DisplayName("A lease dead past the grace window alerts and names the stranded jobs")
    void unrecoveredLeasesAlertWithJobIds() {
        enabled();
        when(jobLease.heartbeatHealth(TTL_SECONDS, GRACE_SECONDS))
                .thenReturn(new JobLeaseRepository.HeartbeatHealth(0, 2, 2));
        when(jobLease.unrecoveredJobUuids(GRACE_SECONDS)).thenReturn(List.of("job-a", "job-b"));

        monitor.reportLeaseHealth();

        verify(metrics).leaseHeartbeats(0, 2, 2);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(eventLogger).trace(message.capture(), any());
        assertTrue(message.getValue().contains("job-a") && message.getValue().contains("job-b"),
                "the alert should name the stranded jobs so they can be chased: " + message.getValue());
        assertTrue(message.getValue().contains(String.valueOf(GRACE_SECONDS)),
                "the alert should say how long the jobs have been dead: " + message.getValue());
    }

    @Test
    @DisplayName("The grace window is a multiple of the lease TTL, so the two stay in step")
    void graceWindowScalesWithTtl() {
        enabled();
        PrototypeProperties props = new PrototypeProperties();
        props.setLeaseGraceMultiplier(5);
        PrototypeLeaseMonitor scaled =
                new PrototypeLeaseMonitor(jobLease, metrics, propertiesService, eventLogger, props, 10);
        when(jobLease.heartbeatHealth(10, 50))
                .thenReturn(new JobLeaseRepository.HeartbeatHealth(1, 0, 0));

        scaled.reportLeaseHealth();

        verify(jobLease).heartbeatHealth(10, 50);
    }

    @Test
    @DisplayName("A database problem in the monitor never takes the worker down with it")
    void databaseFailureIsSwallowed() {
        enabled();
        when(jobLease.heartbeatHealth(anyInt(), anyInt())).thenThrow(new IllegalStateException("db is down"));

        assertDoesNotThrow(() -> monitor.reportLeaseHealth());
        verify(metrics, never()).leaseHeartbeats(anyLong(), anyLong(), anyLong());
    }

    private void enabled() {
        when(propertiesService.isToggleOn(eq(PAUSE_RESUME_PROTOTYPE_ENABLED), eq(false))).thenReturn(true);
    }
}
