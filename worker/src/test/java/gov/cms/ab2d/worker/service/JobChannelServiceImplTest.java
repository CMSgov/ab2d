package gov.cms.ab2d.worker.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import gov.cms.ab2d.worker.processor.JobProgressUpdateService;

class JobChannelServiceImplTest {

  @Test
  void testSendUpdate() {
    JobProgressUpdateService jobProgressUpdateService = mock(JobProgressUpdateService.class);
    JobChannelServiceImpl jobChannelServiceImpl = new JobChannelServiceImpl(jobProgressUpdateService);
    jobChannelServiceImpl.sendUpdate(null, null, 0);
    verify(jobProgressUpdateService).addMeasure(null, null, 0);
  }

}
