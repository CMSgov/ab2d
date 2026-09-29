package gov.cms.ab2d.worker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.processor.coverage.CoverageV3SyncException;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

class WorkerServiceImplTest {

  JobPreProcessor jobPreprocessor = mock(JobPreProcessor.class);
  JobProcessor jobProcessor = mock(JobProcessor.class);
  ShutDownService shutDownService = mock(ShutDownService.class);
  PropertiesService propertiesService = mock(PropertiesService.class);
  CoverageV3Service coverageV3Service = mock(CoverageV3Service.class);

  @ParameterizedTest
  @EnumSource(JobStatus.class)
  void testJobStatus(JobStatus status) {
    Job job = new Job();
    job.setStatus(status);
    when(jobPreprocessor.preprocess(any())).thenReturn(job);

    // assertDoesNotThrow is really the best we can do here... all the function does is log.
    assertDoesNotThrow(() -> {
      WorkerServiceImpl workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService, coverageV3Service);
      workerServiceImpl.process("jobUuid");
    });
  }

  @Test
  void testResetInProgressJobs() {
    WorkerServiceImpl workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService, coverageV3Service);

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

  @Test
  @DisplayName("v3 sync fails if max attempts exceeded (IDR import takes longer than expected)")
  void testV3SyncRetriesExceededIdrImport() {
    val job = createInProgressV3Job("XYZ", "1234");
    when(coverageV3Service.moveFromStagingToRecentCoverage(any(), any())).thenReturn(CoverageV3SyncResult.IDR_IMPORTER_IN_PROGRESS);
    when(jobPreprocessor.preprocess(any())).thenReturn(job);

    val workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService, coverageV3Service);
    workerServiceImpl.setWaitTimeIfIdrImporterInProgress(Duration.ofSeconds(1));

    val exception = assertThrows(CoverageV3SyncException.class, () -> workerServiceImpl.process(job.getJobUuid()));
    assertEquals(exception.getMessage(), "trySyncCoverageV3 failed with IDR_IMPORTER_IN_PROGRESS after 3 attempts");
  }

  @Test
  @DisplayName("v3 sync fails if max attempts exceeded (unable to acquire lock)")
  void testV3SyncRetriesExceededGenericError() {
    val job = createInProgressV3Job("XYZ", "1234");
    when(coverageV3Service.moveFromStagingToRecentCoverage(any(), any())).thenReturn(CoverageV3SyncResult.UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT);
    when(jobPreprocessor.preprocess(any())).thenReturn(job);

    val workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService, coverageV3Service);
    workerServiceImpl.setWaitTimeIfIdrImporterInProgress(Duration.ofSeconds(1));

    val exception = assertThrows(CoverageV3SyncException.class, () -> workerServiceImpl.process(job.getJobUuid()));
    assertEquals(exception.getMessage(), "trySyncCoverageV3 failed with UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT after 3 attempts");
  }

  @ParameterizedTest
  @DisplayName("v3 sync successful")
  @EnumSource(
        value = CoverageV3SyncResult.class,
        names = {"SYNC_SUCCESSFUL_FOR_CONTRACT", "NO_COVERAGE_FOUND_FOR_CONTRACT"}
  )
  void testV3SyncSuccessful(CoverageV3SyncResult result) {
    val job = createInProgressV3Job("XYZ", "1234");
    when(coverageV3Service.moveFromStagingToRecentCoverage(any(), any())).thenReturn(result);
    when(jobPreprocessor.preprocess(any())).thenReturn(job);

    val workerServiceImpl = new WorkerServiceImpl(jobPreprocessor, jobProcessor, shutDownService, propertiesService, coverageV3Service);
    workerServiceImpl.process(job.getJobUuid());
  }

  private Job createInProgressV3Job(String contract, String jobUuid) {
    val job = new Job();
    job.setJobUuid(jobUuid);
    job.setContractNumber(contract);
    job.setFhirVersion(FhirVersion.R4V3);
    job.setStatus(JobStatus.IN_PROGRESS);
    return job;
  }

}
