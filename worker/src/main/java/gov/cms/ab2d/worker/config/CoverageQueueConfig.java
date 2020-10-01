package gov.cms.ab2d.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Build thread pool for new coverage mapping service to use. Distinct pool to avoid thrashing
 * with existing implementation
 */
@Configuration
public class CoverageQueueConfig {

    @Bean(name = "patientCoverageThreadPool")
    public ThreadPoolTaskExecutor patientContractThreadPool(
            @Value("contract.core.pool.size") int corePoolSize,
            @Value("contract.max.pool.size") int maxPoolSize) {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setThreadNamePrefix("coveragep-");
        return taskExecutor;
    }
}
