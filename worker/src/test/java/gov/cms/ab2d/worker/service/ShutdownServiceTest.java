//package gov.cms.ab2d.worker.service;
//
//import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
//import gov.cms.ab2d.eventclient.events.LoggableEvent;
//import gov.cms.ab2d.eventLogger.sendLogsManager;
//import gov.cms.ab2d.job.model.Job;
//import gov.cms.ab2d.job.model.JobStatus;
//import gov.cms.ab2d.job.repository.JobRepository;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//class ShutdownServiceTest {
//
//    @Test
//    void testEmptyReset() {
//        List<String> activeJobs = Collections.emptyList();
//        ShutDownService sds = new ShutDownServiceImpl(buildJobRepository(), new MockEventLogger());
//        sds.resetInProgressJobs(activeJobs);
//        //noinspection ConstantConditions
//        assertTrue(activeJobs.isEmpty());
//    }
//
//    @Test
//    void testResetInProgress() {
//        List<String> activeJobs = Arrays.asList("jobone", "jobtwo");
//        ShutDownService sds = new ShutDownServiceImpl(buildJobRepository(), new MockEventLogger());
//        sds.resetInProgressJobs(activeJobs);
//        assertEquals(2, activeJobs.size());
//    }
//
//    @Test
//    void testException() {
//        List<String> activeJobs = Collections.singletonList("bogus");
//        ShutDownService sds = new ShutDownServiceImpl(buildJobRepository(), null);
//        // Hit the log service
//        sds.resetInProgressJobs(activeJobs);
//        //noinspection ConstantConditions - having one test case makes SonarLint happier.
//        assertFalse(activeJobs.isEmpty());
//    }
//
//    static class MockEventLogger extends LogManager {
//
//        public MockEventLogger() {
//            super(null);
//        }
//
//        @Override
//        public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {
//        }
//    }
//
//    private JobRepository buildJobRepository() {
//        JobRepository retJobRepository = mock(JobRepository.class);
//        when(retJobRepository.findByJobUuid(anyString()))
//                .thenAnswer(jobId -> buildJob(jobId.getArgument(0).toString()));
//        return retJobRepository;
//    }
//
//    private Job buildJob(String jobUuid) {
//        Job retJob = new Job();
//        retJob.setJobUuid(jobUuid);
//        retJob.setStatus(JobStatus.SUBMITTED);
//        return retJob;
//    }
//}
