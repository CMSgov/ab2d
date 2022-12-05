package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.bfd.client.BFDClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures Spring Integration.
 * We are using Spring Integration, because it provides valuable features out of the box, such as
 * database polling, channels, and distributed lock implementation.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableIntegration
@EnableScheduling
@Import(BFDClientConfiguration.class)
// Use @DependsOn to control the loading order so that properties are set before they are used
public class WorkerConfig {

    private final int pcpCorePoolSize;
    private final int jobCorePoolSize;
    private final int jobMaxPoolSize;
    private final int jobQueueCapacity;

    public WorkerConfig(@Value("${pcp.core.pool.size}") int pcpCorePoolSize,
                        @Value("${job.core.pool.size}") int jobCorePoolSize,
                        @Value("${job.max.pool.size}") int jobMaxPoolSize,
                        @Value("${job.queue.capacity}") int jobQueueCapacity) {
        this.pcpCorePoolSize = pcpCorePoolSize;
        this.jobCorePoolSize = jobCorePoolSize;
        this.jobMaxPoolSize = jobMaxPoolSize;
        this.jobQueueCapacity = jobQueueCapacity;
    }

    @Bean
    public RoundRobinBlockingQueue eobClaimRequestsQueue() {
        return new RoundRobinBlockingQueue<>();
    }

    @Bean
    public Executor patientProcessorThreadPool(RoundRobinBlockingQueue eobClaimRequestsQueue) {
        // Regretfully, no good way to supply a custom queue to ThreadPoolTaskExecutor
        // other than by overriding createQueue
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor() {
            @Override
            protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
                return eobClaimRequestsQueue;
            }
        };
        taskExecutor.setCorePoolSize(pcpCorePoolSize);
        // Initially we lock the pool at the minimum size; auto-scaling is done
        // by a separate service.
        taskExecutor.setMaxPoolSize(pcpCorePoolSize);
        taskExecutor.setThreadNamePrefix("pcp-");
        return taskExecutor;
    }

    @Bean(name = "mainJobPool")
    public Executor mainJobProcessingPool() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(jobCorePoolSize);
        taskExecutor.setMaxPoolSize(jobMaxPoolSize);
        taskExecutor.setQueueCapacity(jobQueueCapacity);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        taskExecutor.setThreadNamePrefix("jp-");
        return taskExecutor;
    }

    @Bean
    public LockRepository lockRepository(DataSource dataSource) {
        final DefaultLockRepository defaultLockRepository = new DefaultLockRepository(dataSource);
        defaultLockRepository.setTimeToLive(60_000);        // 60 seconds
        return defaultLockRepository;
    }

    /**
     * Using {@link JdbcLockRegistry} is critical to avoid race condition among workers competing for requests.
     */
    @Bean
    public LockRegistry lockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }
}
