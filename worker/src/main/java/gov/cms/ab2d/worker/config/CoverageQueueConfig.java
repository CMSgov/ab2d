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
            @Value("#{new Integer('${contract.core.pool.size}')}") int corePoolSize,
            @Value("#{new Integer('${contract.max.pool.size}')}") int maxPoolSize) {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setThreadNamePrefix("coveragep-");
        return taskExecutor;
    }

    @Bean
    public CoverageMappingConfig coverageMappingConfig(
            @Value("${coverage.update.months.past}") int pastMonthsToUpdate,
            @Value("${coverage.update.stale.days}") int staleDays,
            @Value("${coverage.update.max.attempts}") int maxAttempts,
            @Value("${coverage.update.stuck.hours}") int stuckHours) {
        return new CoverageMappingConfig(pastMonthsToUpdate, staleDays, maxAttempts, stuckHours);
    }
}
