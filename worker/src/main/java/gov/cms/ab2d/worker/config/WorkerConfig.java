package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.bfd.client.BFDClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
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
public class WorkerConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobHandler handler;

    @Value("${pcp.core.pool.size}")
    private int pcpCorePoolSize;

    @Value("${pcp.max.pool.size}")
    private int pcpMaxPoolSize;

    @Value("${pcp.queue.capacity}")
    private int pcpQueueCapacity;

    @Value("${job.core.pool.size}")
    private int jobCorePoolSize;

    @Value("${job.max.pool.size}")
    private int jobMaxPoolSize;

    @Value("${job.queue.capacity}")
    private int jobQueueCapacity;

    @Bean
    public Executor patientProcessorThreadPool() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(pcpCorePoolSize);
        // Initially we lock the pool at the minimum size; auto-scaling is done
        // by a separate service.
        taskExecutor.setMaxPoolSize(pcpCorePoolSize);
        taskExecutor.setQueueCapacity(pcpQueueCapacity);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setThreadNamePrefix("pcp-");
        return taskExecutor;
    }


    @Bean
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
    public SubscribableChannel channel() {
        return new ExecutorChannel(mainJobProcessingPool());
    }

    @Bean
    public MessageSource<Object> jdbcMessageSource() {
        return new JobMessageSource(dataSource);
    }

    @Bean
    public IntegrationFlow flow() {
        return IntegrationFlows.from(jdbcMessageSource(), c -> c.poller(Pollers.fixedDelay(1000)))
                            .channel(channel())
                            .handle(handler)
                            .get();
    }

    @Bean
    public LockRepository lockRepository() {
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
