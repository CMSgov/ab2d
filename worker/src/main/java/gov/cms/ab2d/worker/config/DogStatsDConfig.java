package gov.cms.ab2d.worker.config;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires a DogStatsD client for emitting Datadog custom metrics over UDP. Defaults target the
 * Datadog Agent running as a local sidecar; the host and port are overridable via the standard
 * Datadog environment variables {@code DD_AGENT_HOST} and {@code DD_DOGSTATSD_PORT}.
 */
@Slf4j
@Configuration
public class DogStatsDConfig {

    @Bean
    public StatsDClient statsDClient(
            @Value("${DD_AGENT_HOST:127.0.0.1}") String agentHost,
            @Value("${DD_DOGSTATSD_PORT:8125}") int dogStatsDPort) {
        log.info("Configuring DogStatsD client targeting {}:{}", agentHost, dogStatsDPort);
        return new NonBlockingStatsDClientBuilder()
                .prefix("ab2d")
                .hostname(agentHost)
                .port(dogStatsDPort)
                .build();
    }
}
