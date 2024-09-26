package gov.cms.ab2d.worker.bfdhealthcheck;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;

class BFDHealthCheckQuartzSetupTest {

  @Test
  void test() {
    String schedule = "0 1 1 ? * *";
    BFDHealthCheckQuartzSetup bfdHealthCheckQuartzSetup = new BFDHealthCheckQuartzSetup(schedule);
    JobDetail jobDetail = mock(JobDetail.class);
    when(jobDetail.getKey()).thenReturn(new JobKey("name"));
    assertDoesNotThrow(() -> {
      bfdHealthCheckQuartzSetup.bfdHealthCheckJobDetail();
      bfdHealthCheckQuartzSetup.bfdHealthCheckJobTrigger(jobDetail);
    });
  }

}
