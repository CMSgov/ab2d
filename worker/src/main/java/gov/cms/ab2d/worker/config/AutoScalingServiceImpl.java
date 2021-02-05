package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.worker.properties.PropertiesChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.worker.config.AutoScalingServiceImpl.Mode.RESET;
import static gov.cms.ab2d.worker.config.AutoScalingServiceImpl.Mode.SCALING_UP;

/**
 * This service is configured with a single {@link Executor} as a parameter that is the subject
 * of auto-scaling. This service itself is a {@link java.util.Timer} task that runs often and
 * monitors
 * activity in the {@link Executor}. Whenever activity is detected, auto-scaling begins and the
 * executor's
 * pool size increases gradually from the core size to max size. Once the activity ceases, the
 * pool size is
 * reset back to the minimums.
 * <br/>
 * <br/>
 * This service only supports {@link ThreadPoolTaskExecutor}s.
 */
@Slf4j
@Service
public class AutoScalingServiceImpl implements AutoScalingService, ApplicationListener<PropertiesChangedEvent> {

    private final ThreadPoolTaskExecutor executor;

    // Can be modified by changing values in properties table
    // in the shared Postgres database
    private int corePoolSize;
    private int maxPoolSize;
    private double scaleToMaxTime;

    private Mode mode = RESET;

    private Instant kickOffTime;

    /**
     * Spring auto-wiring is happening here. Do not change the parameter name.
     *
     * @param patientProcessorThreadPool {@link Executor} to scale up & down.
     */
    public AutoScalingServiceImpl(Executor patientProcessorThreadPool,
                                  @Value("${pcp.core.pool.size}") int corePoolSize,
                                  @Value("${pcp.max.pool.size}") int maxPoolSize,
                                  @Value("${pcp.scaleToMax.time}") int scaleToMaxTime) {
        this.executor = (ThreadPoolTaskExecutor) patientProcessorThreadPool;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.scaleToMaxTime = scaleToMaxTime;
    }

    @Override
    public int getCorePoolSize() {
        return corePoolSize;
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    @Override
    public double getScaleToMaxTime() {
        return scaleToMaxTime;
    }


    @Override
    @Scheduled(fixedDelay = 5000)
    public void autoscale() {
        int activeCount = executor.getActiveCount();
        if (activeCount == 0) {
            // No busy threads -- no active jobs. We can scale back to minimums immediately;
            // no need to do so gradually.
            scaleBackToMin();
        } else {
            scaleUp();
        }
    }

    private void scaleUp() {
        // We need to understand whether we are just kicking off the scale-up procedure
        // or in fact somewhere in the middle/end of it.
        if (mode == RESET) {
            mode = SCALING_UP;
            kickOffTime = Instant.now();
            log.info("Auto-scaling: kicked off at {}", kickOffTime);
        } else if (mode == SCALING_UP) {
            // Auto-scaling is in progress. Increase the pool size if it's time.
            kickItUpANotch();
        }
    }

    private void kickItUpANotch() {
        // We know when we started (see kickOffTime), and we know when we should finish
        // (kickOffTime + scaleToMaxTime). This gives us a rough idea at which pool size
        // we should be at a point in time.
        int currentMaxSize = executor.getMaxPoolSize();
        if (currentMaxSize < maxPoolSize) {   // Maximum pool size is not reached yet.
            // We should be adding a new Thread every N seconds.
            double secondsBetweenScalingEvents = scaleToMaxTime / (maxPoolSize - corePoolSize);
            Duration duration = Duration.between(kickOffTime, Instant.now());
            // At this point in time we want to be at this number of threads.
            int wantedPoolSize = Math.min(maxPoolSize,
                    (int) (corePoolSize + duration.getSeconds() / secondsBetweenScalingEvents));
            if (wantedPoolSize > currentMaxSize) {
                log.info("Auto-scaling: incrementing from {} to {}", currentMaxSize,
                        wantedPoolSize);
                executor.setMaxPoolSize(wantedPoolSize);
            }
        }
    }

    private void scaleBackToMin() {
        if (mode != RESET) {
            // This is all we have to do; ThreadPoolTaskExecutor will shrink automatically.
            executor.setMaxPoolSize(corePoolSize);
            mode = RESET;
            kickOffTime = null;
            log.info("Auto-scaling: reset to core pool size");
        }
    }

    // An event that originates from the PropertiesChangeDetection class
    @Override
    public void onApplicationEvent(PropertiesChangedEvent propertiesChangedEvent) {
        corePoolSize = Integer.parseInt(propertiesChangedEvent.getPropertiesMap().get(PCP_CORE_POOL_SIZE).toString());
        maxPoolSize = Integer.parseInt(propertiesChangedEvent.getPropertiesMap().get(PCP_MAX_POOL_SIZE).toString());
        scaleToMaxTime = Double.parseDouble(propertiesChangedEvent.getPropertiesMap().get(PCP_SCALE_TO_MAX_TIME).toString());

        this.executor.setCorePoolSize(corePoolSize);
    }

    enum Mode {
        SCALING_UP, RESET;
    }
}
