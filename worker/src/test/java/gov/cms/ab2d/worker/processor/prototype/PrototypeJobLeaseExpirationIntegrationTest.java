package gov.cms.ab2d.worker.processor.prototype;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@TestPropertySource(properties = {
    "pause-resume.prototype.lease-renew-ms=100",
    "pause-resume.prototype.max-duration-seconds-create-aggregated-table=1"
})
@ExtendWith({OutputCaptureExtension.class})
class PrototypeJobLeaseExpirationIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    @Test
    @DisplayName("Job lease is not renewed to missing in-memory heartbeat deadline")
    void jobLeaseNotRenewedDueToInMemoryHeartbeatIsMissed(CapturedOutput out) throws Exception {
		// Simulate createAggregatedAttributionTable() longer than expected so that PrototypeJobLeaseRenewer
	    // does not renew the lease token given the in-memory heartbeat deadline is missed
		doAnswer(invocation -> {
            Thread.sleep(2000);
            return null;
        }).when(coverageV3Service).createAggregatedAttributionTable(any());
        val job = createSubmittedV3Job("test");
        val owner = startWorkerUntilOnePartitionDone(job.getJobUuid(), "test-owner");
        owner.run();
	    await()
		    .atMost(5, SECONDS)
		    .pollInterval(100, TimeUnit.MILLISECONDS)
		    .untilAsserted(() -> {
			    assertTrue(out.getOut().contains("Too much time elapsed since last heartbeat - not renewing"));
		    });
    }

    @Test
    @DisplayName("Job lease is removed due to being fenced out")
    void jobLeaseRemovedDueToBeingFencedOut(CapturedOutput out) throws Exception {
        val job = createSubmittedV3Job("test");
		val jobUuid = job.getJobUuid();
        val owner = startWorkerUntilOnePartitionDone(jobUuid, "test-owner");
        owner.run();
        bumpLeaseOutOfBand(job.getJobUuid(), "peer");
        await()
            .atMost(5, SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                assertTrue(out.getOut().contains("Untracking lease token (%s, 1) due to FenceLostException".formatted(jobUuid)));
            });

    }

}
