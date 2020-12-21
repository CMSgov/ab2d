package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverException;
import gov.cms.ab2d.worker.service.WorkerService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EobJobStartupHandlerTest {

    private static final String JOB_UUID = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @Mock
    private JobService jobService;

    @Mock
    private WorkerService workerService;

    @Mock
    private LockRegistry lockRegistry;

    @Mock
    private CoverageDriver coverageDriver;

    @DisplayName("Do not start a job which is not saved in the database")
    @Test
    void checkJobExistsBeforeProcessing() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        when(jobService.getJobByJobUuid(anyString())).thenThrow(ResourceNotFoundException.class);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);

        EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService, jobService, coverageDriver);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "NonExistent");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        GenericMessage<List<Map<String, Object>>> message = new GenericMessage<>(payload);
        MessagingException exception = assertThrows(MessagingException.class,
                () -> eobJobStartupHandler.handleMessage(message));

        assertFalse(lock.isLocked());
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().startsWith("could not find job in database for NonExistent job uuid"));
        assertEquals(ResourceNotFoundException.class, exception.getCause().getClass());

        verify(jobService, times(1)).getJobByJobUuid(anyString());
        verify(coverageDriver, times(0)).isCoverageAvailable(any());
        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(0)).process(any());
    }

    @DisplayName("Job is not started if worker is set to neutral")
    @Test
    void processingNotTriggeredInNeutral() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.NEUTRAL);

        EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService, jobService, coverageDriver);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "DoesNotMatter");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        eobJobStartupHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(jobService, times(0)).getJobByJobUuid(anyString());
        verify(coverageDriver, times(0)).isCoverageAvailable(any());
        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(0)).process(any());
    }

    @DisplayName("Job is not started if coverage is not available")
    @Test
    void proccessingNotTriggeredIfCoverageNotAvailable() {

        ReentrantLock lock = new ReentrantLock();
        try {
            when(jobService.getJobByJobUuid(anyString())).thenReturn(new Job());
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(coverageDriver.isCoverageAvailable(any())).thenReturn(false);

            EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService, jobService, coverageDriver);

            Map<String, Object> jobMap = new HashMap<>() {{
                put("job_uuid", "Exists Yay");
            }};
            List<Map<String, Object>> payload = List.of(jobMap);

            eobJobStartupHandler.handleMessage(new GenericMessage<>(payload));

            assertFalse(lock.isLocked());

            verify(jobService, times(1)).getJobByJobUuid(anyString());
            verify(coverageDriver, times(1)).isCoverageAvailable(any());
            verify(workerService, times(1)).getEngagement();
            verify(workerService, times(0)).process(any());

        } catch (InterruptedException interruptedException) {
            // Will never be thrown but just in case log it
            interruptedException.printStackTrace();
        }
    }

    @DisplayName("Job is not started if coverage check is interrupted")
    @Test
    void proccessingNotTriggeredIfCoverageCheckInterrupted() {

        ReentrantLock lock = new ReentrantLock();

        try {
            when(jobService.getJobByJobUuid(anyString())).thenReturn(new Job());
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(coverageDriver.isCoverageAvailable(any())).thenThrow(InterruptedException.class);

            EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService, jobService, coverageDriver);

            Map<String, Object> jobMap = new HashMap<>() {{
                put("job_uuid", "Exists Yay");
            }};
            List<Map<String, Object>> payload = List.of(jobMap);

            MessagingException exception = assertThrows(MessagingException.class,
                    () -> eobJobStartupHandler.handleMessage(new GenericMessage<>(payload)));

            assertFalse(lock.isLocked());
            assertEquals(InterruptedException.class, exception.getCause().getClass());
            assertTrue(exception.getMessage().startsWith("could not determine whether coverage metadata was up to date"));

            verify(jobService, times(1)).getJobByJobUuid(anyString());
            verify(coverageDriver, times(1)).isCoverageAvailable(any());
            verify(workerService, times(1)).getEngagement();
            verify(workerService, times(0)).process(any());
        } catch (InterruptedException interruptedException) {
            // Will never be thrown but just in case log it
            interruptedException.printStackTrace();
        }
    }

    @DisplayName("Job is not started if coverage check is interrupted")
    @Test
    void proccessingNotTriggeredIfCoverageCheckFails() {

        ReentrantLock lock = new ReentrantLock();

        try {
            when(jobService.getJobByJobUuid(anyString())).thenReturn(new Job());
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(coverageDriver.isCoverageAvailable(any())).thenThrow(CoverageDriverException.class);

            EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService, jobService, coverageDriver);

            Map<String, Object> jobMap = new HashMap<>() {{
                put("job_uuid", "Exists Yay");
            }};
            List<Map<String, Object>> payload = List.of(jobMap);

            MessagingException exception = assertThrows(MessagingException.class,
                    () -> eobJobStartupHandler.handleMessage(new GenericMessage<>(payload)));

            assertFalse(lock.isLocked());
            assertEquals(CoverageDriverException.class, exception.getCause().getClass());
            assertTrue(exception.getMessage().startsWith("could not check coverage due to unexpected exception"));

            verify(jobService, times(1)).getJobByJobUuid(anyString());
            verify(coverageDriver, times(1)).isCoverageAvailable(any());
            verify(workerService, times(1)).getEngagement();
            verify(workerService, times(0)).process(any());
        } catch (InterruptedException interruptedException) {
            // Will never be thrown but just in case log it
            interruptedException.printStackTrace();
        }
    }

    @DisplayName("Attempt to start a job if job uuid is present")
    @Test
    void triggerProcessingJob() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        when(jobService.getJobByJobUuid(anyString())).thenReturn(new Job());
        when(lockRegistry.obtain(anyString())).thenReturn(lock);
        when(coverageDriver.isCoverageAvailable(any())).thenReturn(true);

        EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService, jobService, coverageDriver);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "Exists Yay");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        eobJobStartupHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(jobService, times(1)).getJobByJobUuid(anyString());
        verify(coverageDriver, times(1)).isCoverageAvailable(any());
        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(1)).process(any());
    }
}
