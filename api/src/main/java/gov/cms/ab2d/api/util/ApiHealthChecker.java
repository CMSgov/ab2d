package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.NewRelic;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ApiHealthChecker {

    private static final String HEALTH_URL = "https://api.ab2d.cms.gov/health";

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth() {
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
            log.warn("API health check FAILED (status={})", status);
        } else {
            log.info("API health check OK (status={})", status);
        }
    }
}
