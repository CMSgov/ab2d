package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
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
class JobHandlerTest {

    @Mock
    private WorkerService workerService;

    @Mock
    private LockRegistry lockRegistry;

    @DisplayName("Job is not started if worker is set to neutral")
    @Test
    void processingNotTriggeredInNeutral() {

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.NEUTRAL);

        JobHandler jobHandler = new JobHandler(lockRegistry, workerService);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "DoesNotMatter");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        jobHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(0)).process(anyString());
    }

    @Test
    void testEmptyPayload() {

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);

        JobHandler jobHandler = new JobHandler(lockRegistry, workerService);

        List<Map<String, Object>> payload = List.of();

        jobHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(0)).process(anyString());
    }


    @DisplayName("Job is started if worker is set to in gear")
    @Test
    void processingTriggeredInGear() {

        Job submittedJob = new Job();
        submittedJob.setStatus(JobStatus.IN_PROGRESS);

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);
        when(workerService.process(anyString())).thenReturn(submittedJob);

        JobHandler jobHandler = new JobHandler(lockRegistry, workerService);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "DoesNotMatter");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        jobHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(1)).process(anyString());
    }

    @Test
    void testResourceNotFoundException() {
        Job submittedJob = new Job();
        submittedJob.setStatus(JobStatus.IN_PROGRESS);

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);
        when(workerService.process(anyString())).thenThrow(ResourceNotFoundException.class);

        JobHandler jobHandler = new JobHandler(lockRegistry, workerService);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "DoesNotMatter");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);
        GenericMessage<List<Map<String, Object>>> message = new GenericMessage<>(payload);

        assertThrows(
            MessagingException.class,
            () -> {
                jobHandler.handleMessage(message);
            }
        );
    }

    @DisplayName("Handler attempts to start jobs until it finds one that it can start")
    @Test
    void processUntilSuccessfulForAJob() {

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);

        Job submittedJob = new Job();
        submittedJob.setStatus(JobStatus.SUBMITTED);
        Job startedJob = new Job();
        startedJob.setStatus(JobStatus.IN_PROGRESS);


        when(workerService.process(anyString())).thenReturn(submittedJob, submittedJob, startedJob, startedJob);

        JobHandler jobHandler = new JobHandler(lockRegistry, workerService);

        Map<String, Object> first = new HashMap<>() {{
            put("job_uuid", "first job id");
        }};

        Map<String, Object> second = new HashMap<>() {{
            put("job_uuid", "second job id");
        }};

        Map<String, Object> third = new HashMap<>() {{
            put("job_uuid", "third job id");
        }};

        Map<String, Object> fourth = new HashMap<>() {{
            put("job_uuid", "fourth job id");
        }};

        List<Map<String, Object>> payload = List.of(first, second, third, fourth);

        jobHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(3)).process(anyString());
    }
}


