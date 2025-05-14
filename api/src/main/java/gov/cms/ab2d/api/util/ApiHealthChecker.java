package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.NewRelic;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ApiHealthChecker {
    private String HEALTH_URL;

    public ApiHealthChecker(@Value("${execution.env}") String ab2dEnv) {
        Ab2dEnvironment ab2dEnvironment = Ab2dEnvironment.fromName(ab2dEnv);

        switch (ab2dEnvironment) {
            case IMPL -> HEALTH_URL = "https://impl.ab2d.cms.gov/health";
            case SANDBOX -> HEALTH_URL = "https://sandbox.ab2d.cms.gov/health";
            case PRODUCTION -> HEALTH_URL = "https://api.ab2d.cms.gov/health";
        }
    }

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth() {
        if (HEALTH_URL != null && !HEALTH_URL.isEmpty()) {

            boolean success = false;
            int status = -1;

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(HEALTH_URL);

                status = client.execute(request, HttpResponse::getCode);
                success = (status >= 200 && status < 300);
            } catch (IOException e) {
                NewRelic.noticeError(e);
                log.error("Health check failed: {}", e.getMessage());
            }

            // Record a custom event in New Relic
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("url", HEALTH_URL);
            attrs.put("statusCode", status);
            attrs.put("success", success);
            NewRelic.getAgent().getInsights().recordCustomEvent("ApiHealthCheck", attrs);

            if (!success) {
                NewRelic.noticeError("API unavailable, status=" + status);
                log.error("API health check FAILED (status={})", status);
            } else {
                log.error("API health check OK (status={})", status);
            }
        }
    }
}
