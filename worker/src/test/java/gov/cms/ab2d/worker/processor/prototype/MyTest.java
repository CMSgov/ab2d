package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
        // we don't need a delay for this test
        "pause-resume.prototype.lease-renew-ms=100",
        "pause-resume.prototype.max-duration-seconds-create-aggregated-table=1"
})
class MyTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MyTest.class);

    @Test
    @DisplayName("create aggregated table -- TODO")
    void blah() throws Exception {

        doAnswer(invocation -> {
            Thread.sleep(3000);
            return null;
        }).when(coverageV3Service).createAggregatedAttributionTable(any());

        Job job = createSubmittedV3Job("test");
        String uuid = job.getJobUuid();

        // Owner A runs until one partition has COMPLETED, then its thread is abandoned
        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-owner");

        owner.run();

    }

    @Test
    @DisplayName("bump lease -- TODO")
    void blah2() throws Exception {
        Job job = createSubmittedV3Job("test");
        String uuid = job.getJobUuid();

        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-owner");

        bumpLeaseOutOfBand(job.getJobUuid(), "peer");
        owner.run();
    }

}
