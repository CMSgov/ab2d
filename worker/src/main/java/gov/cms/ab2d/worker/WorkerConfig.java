package gov.cms.ab2d.worker;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Slf4j
@Configuration
@EnableIntegration
/**
 * Configures Spring Integration.
 * We are using Spring Integration, because it provides valuable features out of the box, such as
 * database polling, channels, and distributed lock implementation.
 */
public class WorkerConfig {

//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private JobHandler handler;

//    @Bean
//    /**
//     * In a Production system we would probably have a channel that feeds a pool of {@link java.util.concurrent.Executor}s,
//     * but for the purposes of this excercise we are keeping things simple and using a {@link DirectChannel}.
//     */
//    public SubscribableChannel channel() {
//        return new DirectChannel();
//    }


    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(25);
        return taskExecutor;
    }


    @Bean
    public SubscribableChannel channel() {
        return new ExecutorChannel(threadPoolTaskExecutor());
    }



//    private static final String query = "    SELECT * " +
//            "      FROM job j                                                                                                                    " +
//            "     WHERE j.status = 'SUBMITTED'  " +
//            "       AND j.status_message ='0%' " +
//            "       AND (SELECT count(lock_key) " +
//            "            FROM int_lock l " +
//            "           WHERE l.lock_key = j.job_id) = 0 " +
//            "  ORDER BY j.created_at; ";
//
//    @Bean
//    /**
//     * The "request" table essentially becomes a queue of messages that get polled here.
//     */
//    public MessageSource<Object> jdbcMessageSource() {
//        final JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(dataSource, query);
//        adapter.setMaxRows(1);
//        adapter.setRowMapper(new RowMapper<Job>() {
//            @Override
//            public Job mapRow(ResultSet row, int rowNum) throws SQLException {
//                Job job = new Job();
//                job.setId(row.getLong("id"));
//                job.setJobId(row.getString("job_id"));
////                job.setCreatedAt(OffsetDateTime.parse(row.getString("created_at")));
//                job.setCreatedAt(OffsetDateTime.now());
//
////                final String expires_at = String.valueOf(row.getString("expires_at"));
//                job.setExpiresAt(OffsetDateTime.now());
//
//                job.setResourceTypes(row.getString("resource_types"));
//
////              job.setStatus(row.get("status"));
//                job.setStatusMessage(row.getString("status_message"));
//
//                log.info(" ################################################################################");
//                log.info(" ROW SEARIALIZED TO Job instance : {} ", job);
//                log.info(" ################################################################################");
//
//                return job;
//            }
//        });
//        return adapter;
//    }


    @Bean
    public MessageSource<Object> jdbcMessageSource(DataSource dataSource) {
        return new JobMessageSource(dataSource);
    }

    @Bean
    public IntegrationFlow pollingFlow(DataSource dataSource) {
        return IntegrationFlows.from(jdbcMessageSource(dataSource),
                c -> c.poller(Pollers.fixedDelay(1000)))
                .channel(channel())
                .get();
    }

    @Bean
    public IntegrationFlow hanldingFlow(JobHandler handler) {
        return IntegrationFlows.from("channel")
                .handle(handler)
                .get();
    }

    @Bean
    public LockRepository lockRepository(DataSource dataSource) {
        return new DefaultLockRepository(dataSource);
    }

    @Bean
    /**
     * Using {@link JdbcLockRegistry} is critical to avoid race condition among workers competing for requests.
     * Locks will auto-expire after one hour.
     */
    public LockRegistry lockRegistry(LockRepository lockRepository) {
        final JdbcLockRegistry registry = new JdbcLockRegistry(lockRepository);
        registry.expireUnusedOlderThan(DateUtils.MILLIS_PER_HOUR);
        return registry;
    }
}
