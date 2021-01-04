package gov.cms.ab2d.worker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.*;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Configures Spring Integration.
 * We are using Spring Integration, because it provides valuable features out of the box, such as
 * database polling, channels, and distributed lock implementation.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableIntegration
public class WorkerFlowConfig {

    private final DataSource dataSource;

    private final JobHandler handler;

    private final Executor mainJobPool;

    // How often we poll for matching jobs in the job table
    private final int pollingFrequency;

    public WorkerFlowConfig(DataSource dataSource, JobHandler handler,
                            @Qualifier("mainJobPool") Executor mainJobPool,
                            @Value("${eob.job.queueing.frequency}") int pollingFrequency) {
        this.dataSource = dataSource;
        this.handler = handler;
        this.mainJobPool = mainJobPool;
        this.pollingFrequency = pollingFrequency;
    }

    @Bean
    public IntegrationFlow flow() {
        return IntegrationFlows.from(new JobMessageSource(dataSource), c -> c.poller(Pollers.fixedDelay(pollingFrequency, TimeUnit.SECONDS)))
                            .channel(new ExecutorChannel(mainJobPool))
                            .handle(handler)
                            .get();
    }
}
