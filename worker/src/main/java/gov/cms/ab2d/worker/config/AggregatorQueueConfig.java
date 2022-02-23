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
public class AggregatorQueueConfig {

    @Bean(name = "aggregatorThreadPool")
    public ThreadPoolTaskExecutor aggregatorThreadPool(
            @Value("#{new Integer('${coverage.core.pool.size}')}") int corePoolSize) {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(corePoolSize);
        taskExecutor.setThreadNamePrefix("aggregator-");
        taskExecutor.initialize();

       return taskExecutor;
    }
}
