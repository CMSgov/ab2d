package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.bfd.client.BFDClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.*;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.concurrent.Executor;

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
@DependsOn("propertiesInit")
public class WorkerFlowConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobHandler handler;

    @Autowired
    @Qualifier("mainJobPool")
    private Executor mainJobPool;

    @Bean
    public SubscribableChannel channel() {
        return new ExecutorChannel(mainJobPool);
    }

    @Bean
    public IntegrationFlow flow() {
        return IntegrationFlows.from(new JobMessageSource(dataSource), c -> c.poller(Pollers.fixedDelay(1000)))
                            .channel(channel())
                            .handle(handler)
                            .get();
    }
}
