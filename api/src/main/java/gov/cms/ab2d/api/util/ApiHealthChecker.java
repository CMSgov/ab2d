package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.NewRelic;
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

    private final Ab2dEnvironment ab2dEnvironment;

    public ApiHealthChecker(@Value("${execution.env}") String ab2dEnv) {
        this.ab2dEnvironment = Ab2dEnvironment.fromName(ab2dEnv);
    }

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth() {
        log.info("Checking API health");
        // Record a custom event in New Relic
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("environment", ab2dEnvironment);
        attrs.put("success", "success");
        NewRelic.getAgent().getInsights().recordCustomEvent("ApiHealthCheck", attrs);
    }
}

