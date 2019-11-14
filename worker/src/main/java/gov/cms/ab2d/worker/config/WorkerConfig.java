package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.bfd.client.BFDClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;

/**
 * Configures Spring Integration.
 * We are using Spring Integration, because it provides valuable features out of the box, such as
 * database polling, channels, and distributed lock implementation.
 */
@Slf4j
@Configuration
@EnableIntegration
@Import(BFDClientConfiguration.class)
public class WorkerConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobHandler handler;


    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(0);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(30);
        return taskExecutor;
    }


    @Bean
    public SubscribableChannel channel() {
        return new ExecutorChannel(threadPoolTaskExecutor());
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
        return new DefaultLockRepository(dataSource);
    }

    /**
     * Using {@link JdbcLockRegistry} is critical to avoid race condition among workers competing for requests.
     * Locks will auto-expire after one hour.
     */
    @Bean
    public LockRegistry lockRegistry(LockRepository lockRepository) {
        final JdbcLockRegistry registry = new JdbcLockRegistry(lockRepository);
        registry.expireUnusedOlderThan(DateUtils.MILLIS_PER_HOUR);
        return registry;
    }
}
