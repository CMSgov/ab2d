package gov.cms.ab2d.api.util;

import com.timgroup.statsd.StatsDClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ApiHealthChecker {

    static final String HEALTH_CHECK_EVENT = "ApiHealthCheck";

    /** Resolves (with the "ab2d" client prefix) to the Datadog metric {@code ab2d.api.health}. */
    static final String HEALTH_GAUGE = "api.health";

    private final Ab2dEnvironment ab2dEnvironment;
    private final StatsDClient statsDClient;

    public ApiHealthChecker(@Value("${execution.env}") String ab2dEnv, StatsDClient statsDClient) {
        this.ab2dEnvironment = Ab2dEnvironment.fromName(ab2dEnv);
        this.statsDClient = statsDClient;
    }

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth() {
        log.info("Checking API health");

        // (a) structured log line for analytics (replaces the New Relic ApiHealthCheck Insights event)
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("environment", ab2dEnvironment.getName());
        attrs.put("success", true);
        log.info("{} {}", HEALTH_CHECK_EVENT, attrs);

        // (b) DogStatsD gauge (no parent trace context) tagged with environment
        statsDClient.gauge(HEALTH_GAUGE, 1, "environment:" + ab2dEnvironment.getName());
    }
}

