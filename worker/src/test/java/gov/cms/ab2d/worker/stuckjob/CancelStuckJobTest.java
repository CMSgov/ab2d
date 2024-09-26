package gov.cms.ab2d.worker.stuckjob;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;

class CancelStuckJobTest {

  @Test
  void test() throws Exception {
    CancelStuckJobsProcessor cancelStuckJobsProcessor = mock(CancelStuckJobsProcessor.class);
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    CancelStuckJob cancelStuckJob = new CancelStuckJob(cancelStuckJobsProcessor);
    cancelStuckJob.executeInternal(jobExecutionContext);
    verify(cancelStuckJobsProcessor).process();
  }

}
