package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.worker.service.WorkerService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.support.GenericMessage;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EobJobStartupHandlerTest {

    private static final String JOB_UUID = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @Mock
    private WorkerService workerService;

    @Mock
    private LockRegistry lockRegistry;

    @DisplayName("Job is not started if worker is set to neutral")
    @Test
    void processingNotTriggeredInNeutral() {

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.NEUTRAL);

        EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "DoesNotMatter");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        eobJobStartupHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(0)).process(any());
    }

    @DisplayName("Job is started if worker is set to in gear")
    @Test
    void processingTriggeredInGear() {

        ReentrantLock lock = new ReentrantLock();
        when(workerService.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);

        EobJobStartupHandler eobJobStartupHandler = new EobJobStartupHandler(lockRegistry, workerService);

        Map<String, Object> jobMap = new HashMap<>() {{
            put("job_uuid", "DoesNotMatter");
        }};
        List<Map<String, Object>> payload = List.of(jobMap);

        eobJobStartupHandler.handleMessage(new GenericMessage<>(payload));

        assertFalse(lock.isLocked());

        verify(workerService, times(1)).getEngagement();
        verify(workerService, times(1)).process(any());
    }
}


