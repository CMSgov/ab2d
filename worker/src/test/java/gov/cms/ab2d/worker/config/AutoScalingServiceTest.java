package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Future;

import gov.cms.ab2d.common.properties.PropertyServiceStub;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gov.cms.ab2d.common.util.PropertyConstants.*;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Set property.change.detection to false, otherwise the values from the database will override the values that are being hardcoded here.
@SpringBootTest(properties = {"pcp.core.pool.size=3", "pcp.max.pool.size=20", "pcp.scaleToMax.time=20", "property.change.detection=false"})
@Testcontainers
@Slf4j
@Import(AB2DSQSMockConfig.class)
public class AutoScalingServiceTest {

    public static final int QUEUE_SIZE = 25;
    public static final int MAX_POOL_SIZE = 20;
    public static final int MIN_POOL_SIZE = 20;

    @Autowired
    private ThreadPoolTaskExecutor patientProcessorThreadPool;

    @Autowired
    private ApplicationContext context;

    private AutoScalingServiceImpl autoScalingService;

    @Autowired
    private RoundRobinBlockingQueue eobClaimRequestsQueue;

    private PropertiesService propertiesService = new PropertyServiceStub();

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DCoveragePostgressqlContainer();

    private int originalMaxPoolSize;

    @BeforeEach
    public void init() {
        patientProcessorThreadPool.getThreadPoolExecutor().getQueue().clear();
        autoScalingService = new AutoScalingServiceImpl(patientProcessorThreadPool,
                eobClaimRequestsQueue, propertiesService, 3, 20, 20);
        originalMaxPoolSize = autoScalingService.getMaxPoolSize();
        patientProcessorThreadPool.setMaxPoolSize(originalMaxPoolSize);
        AutoScalingService asservice = context.getBean(AutoScalingServiceImpl.class);
        ReflectionTestUtils.setField(asservice, "propertiesService", propertiesService);
        propertiesService.createProperty(MAINTENANCE_MODE, "false");
        propertiesService.createProperty(PCP_MAX_POOL_SIZE, "" + originalMaxPoolSize);
        propertiesService.createProperty(PCP_SCALE_TO_MAX_TIME, "20");
        propertiesService.createProperty(PCP_CORE_POOL_SIZE, "3");
    }

    @AfterEach
    public void cleanup() {
        patientProcessorThreadPool.getThreadPoolExecutor().getQueue().clear();
        ((PropertyServiceStub) propertiesService).reset();
        propertiesService.updateProperty(MAINTENANCE_MODE, "false");
        propertiesService.updateProperty(PCP_MAX_POOL_SIZE, "" + originalMaxPoolSize);
    }

    @Test
    @DisplayName("Auto-scaling does not kick in when maintenance mode is enabled")
    void maintenanceModeNoAutoScaling() throws InterruptedException {

        final List<Future> futures = new ArrayList<>();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set("TEST_CONTRACT");
        for (int i = 0; i < QUEUE_SIZE; i++) {
            futures.add(patientProcessorThreadPool.submit(sleepyRunnable()));
        }
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();

        Thread.sleep(1000);

        // Starts at three will scale once there is work in queue
        assertEquals(20, patientProcessorThreadPool.getMaxPoolSize());
        assertEquals(3, patientProcessorThreadPool.getCorePoolSize());

        propertiesService.updateProperty(PCP_MAX_POOL_SIZE, "4");

        Thread.sleep(10000);

        propertiesService.updateProperty(MAINTENANCE_MODE, "true");

        Thread.sleep(6000);

        assertEquals(3, patientProcessorThreadPool.getMaxPoolSize());
        assertEquals(3, patientProcessorThreadPool.getCorePoolSize());

        futures.forEach(future -> future.cancel(true));
    }

    @Test
    @DisplayName("Auto-scaling does not kick in when the queue remains empty")
    void emptyQueueNoAutoScaling() throws InterruptedException {
        // Verify that initially the pool is sized at the minimums
        assertEquals(originalMaxPoolSize, patientProcessorThreadPool.getMaxPoolSize());
        assertEquals(3, patientProcessorThreadPool.getCorePoolSize());

        // Auto-scaling should not kick in while the queue is empty
        Thread.sleep(7000);
        assertEquals(originalMaxPoolSize, patientProcessorThreadPool.getMaxPoolSize());
        assertEquals(3, patientProcessorThreadPool.getCorePoolSize());

    }

    @Test
    @DisplayName("Auto-scaling kicks in and resizes the pool")
    void autoScalingKicksInAndResizes() throws InterruptedException {
        // Make the Executor busy.
        final List<Future<?>> futures = new ArrayList<>();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set("TEST_CONTRACT");
        for (int i = 0; i < QUEUE_SIZE; i++) {
            futures.add(patientProcessorThreadPool.submit(sleepyRunnable()));
        }
        RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();

        // In approximately 20 seconds the Executor should grow to the max.
        var start = Instant.now();
        LinkedHashSet<Integer> metrics = new LinkedHashSet<>();
        for (int i = 0; i < 80; i++) {
            int size =
                    patientProcessorThreadPool.getThreadPoolExecutor().getMaximumPoolSize();
            metrics.add(size);
            log.info("Pool size: {}", size);
            if (size >= MAX_POOL_SIZE) {
                break;
            }
            Thread.sleep(500);
        }
        var end = Instant.now();

        // The pool should have grown to 20.
        assertEquals(MAX_POOL_SIZE, patientProcessorThreadPool.getMaxPoolSize());

        // How do we actually verify that pool growth was in fact gradual and not instantaneous?
        // First, check that there was the expected time gap between auto scaling start & end
        assertTrue(Duration.between(start, end).getSeconds() >= 0);

        // Then check that there were intermediate pool increases between 3 and MAX_POOL_SIZE.
        // Last metric taken should always be MAX_POOL_SIZE
        assertEquals(MAX_POOL_SIZE, new ArrayDeque<>(metrics).getLast());

        // There are 3 intermediate metrics and 1 final metric
        assertTrue(metrics.size() >= 1);
        List<Integer> metricsList = new ArrayList<>(metrics);
        for (int i = 1; i < metricsList.size(); i++) {
            assertTrue(metricsList.get(i - 1) < metricsList.get(i));
        }

        // We need to make sure it does not grow beyond the max though. Let's sleep for a bit
        // and verify the size is still at the same max value.
        Thread.sleep(8000);
        assertEquals(MAX_POOL_SIZE, patientProcessorThreadPool.getMaxPoolSize());

        // Clean up.
        futures.forEach(future -> future.cancel(true));

        // Sleep for a bit to let auto scaling run another cycle. The max pool size should be
        // reverted
        // back to the original value after that.
        Thread.sleep(10000);
        assertEquals(MIN_POOL_SIZE, patientProcessorThreadPool.getMaxPoolSize());
        assertEquals(0, patientProcessorThreadPool.getThreadPoolExecutor().getActiveCount());
    }

    @Test
    void testGetIntProperty() {
        PropertiesService mockPropertiesService = mock(PropertiesService.class);
        AutoScalingServiceImpl mockedAutoScalingService = new AutoScalingServiceImpl(
            patientProcessorThreadPool, eobClaimRequestsQueue, mockPropertiesService, 3, 20, 20
        );
        when(mockPropertiesService.getProperty(anyString(), anyString())).thenThrow(NullPointerException.class);
        assertEquals(99, mockedAutoScalingService.getIntProperty("property", 99));
    }

    @Test
    void testGetDoubleProperty() {
        PropertiesService mockPropertiesService = mock(PropertiesService.class);
        AutoScalingServiceImpl mockedAutoScalingService = new AutoScalingServiceImpl(
            patientProcessorThreadPool, eobClaimRequestsQueue, mockPropertiesService, 3, 20, 20
        );
        when(mockPropertiesService.getProperty(anyString(), anyString())).thenThrow(NullPointerException.class);
        assertEquals(99, mockedAutoScalingService.getDoubleProperty("property", 99));
    }

    @Test
    void testGetBooleanProperty() {
        PropertiesService mockPropertiesService = mock(PropertiesService.class);
        AutoScalingServiceImpl mockedAutoScalingService = new AutoScalingServiceImpl(
            patientProcessorThreadPool, eobClaimRequestsQueue, mockPropertiesService, 3, 20, 20
        );
        when(mockPropertiesService.isToggleOn(anyString(), anyBoolean())).thenThrow(NullPointerException.class);
        assertFalse(mockedAutoScalingService.getBooleanProperty("property", false));
    }

    private Runnable sleepyRunnable() {
        return () -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // NO-OP
            }
        };
    }
}
