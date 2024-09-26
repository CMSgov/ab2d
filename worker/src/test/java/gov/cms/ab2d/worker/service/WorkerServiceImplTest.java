package gov.cms.ab2d.worker.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;

import java.util.List;
import java.util.ArrayList;

class WorkerServiceImplTest {

  @ParameterizedTest
  @EnumSource(JobStatus.class)
  void testJobStatus(JobStatus status) {
    JobPreProcessor jobPreprocessor = mock(JobPreProcessor.class);
    JobProcessor jobProcessor = mock(JobProcessor.class);
    ShutDownService shutDownService = mock(ShutDownService.class);
    PropertiesService propertiesService = mock(PropertiesService.class);

    Job job = new Job();
    job.setStatus(status);
    when(jobPreprocessor.preprocess(any())).thenReturn(job);

    // assertDoesNotThrow is really the best we can do here... all the function does is log.
    assertDoesNotThrow(() -> {
      WorkerServiceImpl workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService);
      workerServiceImpl.process("jobUuid");
    });
  }

  @Test
  void testResetInProgressJobs() {
    JobPreProcessor jobPreprocessor = mock(JobPreProcessor.class);
    JobProcessor jobProcessor = mock(JobProcessor.class);
    ShutDownService shutDownService = mock(ShutDownService.class);
    PropertiesService propertiesService = mock(PropertiesService.class);

    WorkerServiceImpl workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService);

    // verify "resetInProgressJobs" wasn't called, because "activeJobs" is empty
    workerServiceImpl.resetInProgressJobs();
    verify(shutDownService, never()).resetInProgressJobs(any());

    // verify "resetInProgressJobs" has been called once, because "activeJobs" is no longer empty
    List<String> activeJobs = new ArrayList<>();
    activeJobs.add("jobUuid");
    ReflectionTestUtils.setField(workerServiceImpl, "activeJobs", activeJobs);
    workerServiceImpl.resetInProgressJobs();
    verify(shutDownService, times(1)).resetInProgressJobs(any());
  }
}
