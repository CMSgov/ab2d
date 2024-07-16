package gov.cms.ab2d.worker.processor.coverage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import gov.cms.ab2d.worker.service.coveragesnapshot.CoverageSnapshotService;

class CoverageProcessorImplTest {

  @Test
  void testQueueCoveragePeriod() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);
    CoveragePeriod coveragePeriod = mock(CoveragePeriod.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.queueCoveragePeriod(coveragePeriod, true);
    });
  }

  @Test
  void testStartJob() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);
    CoverageMapping coverageMapping = mock(CoverageMapping.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.startJob(coverageMapping);
    });
  }


  @Test
  void testIsProcessorBusy() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.isProcessorBusy();
    });
  }

  @Test
  void testQueueMapping() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);
    CoverageMapping coverageMapping = mock(CoverageMapping.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.queueMapping(coverageMapping, true);
    });
  }

  @Test
  void testMonitorMappingJobs() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.monitorMappingJobs();
    });
  }

  @Test
  void testEvaluateJob() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);
    CoverageMapping coverageMapping = mock(CoverageMapping.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.evaluateJob(coverageMapping);
    });
  }

  @Test
  void testInsertJobResults() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.insertJobResults();
    });
  }

  @Test
  void testShutdown() {
    CoverageService coverageService = mock(CoverageService.class);
    BFDClient bfdClient = mock(BFDClient.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ContractWorkerClient contractWorkerClient = mock(ContractWorkerClient.class);
    CoverageSnapshotService coverageSnapshotService = mock(CoverageSnapshotService.class);

    CoverageProcessorImpl coverageProcessorImpl = new CoverageProcessorImpl(
      coverageService, bfdClient, executor, 1, contractWorkerClient, coverageSnapshotService
    );

    assertDoesNotThrow(() -> {
      coverageProcessorImpl.shutdown();
    });
  }

}
