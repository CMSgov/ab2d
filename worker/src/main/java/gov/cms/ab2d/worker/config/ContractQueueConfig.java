package gov.cms.ab2d.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Build thread pool used to execute work concerning contract mapping in ContractBeneSearchImpl
 */
@Configuration
public class ContractQueueConfig {

    @Bean(name = "patientContractThreadPool")
    public ThreadPoolTaskExecutor patientContractThreadPool(
            @Value("${contract.core.pool.size}") Integer corePoolSize,
            @Value("${contract.max.pool.size}") Integer maxPoolSize) {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setThreadNamePrefix("contractp-");
        return taskExecutor;
    }
}
