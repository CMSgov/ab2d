package gov.cms.ab2d.worker.bfdhealthcheck;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;

class BFDHealthCheckJobTest {

  @Test
  void test() {
    BFDHealthCheck bfhHealthCheck = mock(BFDHealthCheck.class);
    BFDHealthCheckJob bfdHealthCheckJob = new BFDHealthCheckJob(bfhHealthCheck);
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    bfdHealthCheckJob.executeInternal(jobExecutionContext);
    verify(bfhHealthCheck).checkBFDHealth();
  }

}
