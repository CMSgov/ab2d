package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.NewRelic;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ApiHealthChecker {
    private final String healthUrl;

    public ApiHealthChecker(@Value("${execution.env}") String ab2dEnv) {
        Ab2dEnvironment ab2dEnvironment = Ab2dEnvironment.fromName(ab2dEnv);

        healthUrl = switch (ab2dEnvironment) {
            case IMPL -> "https://impl.ab2d.cms.gov/health";
            case SANDBOX -> "https://sandbox.ab2d.cms.gov/health";
            case PRODUCTION -> "https://api.ab2d.cms.gov/health";
            default -> "";
        };
        log.error("API ENV  " + healthUrl);
    }

    private final RequestConfig reqCfg = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(7))
            .build();

    private final CloseableHttpClient client = HttpClients.custom()
            .setDefaultRequestConfig(reqCfg)
            .build();

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth() {
        if (healthUrl != null && !healthUrl.isEmpty()) {

            boolean success = false;
            int status = -1;

            try {
                HttpGet request = new HttpGet(healthUrl);

                status = client.execute(request, HttpResponse::getCode);
                log.error("API status " + status);
                success = (status >= 200 && status < 300);
            } catch (IOException e) {
                NewRelic.noticeError(e);
                log.error("Health check failed for URL " + healthUrl + " — see stack trace:" + e);
            }

            // Record a custom event in New Relic
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("url", healthUrl);
            attrs.put("statusCode", status);
            attrs.put("success", success);
            NewRelic.getAgent().getInsights().recordCustomEvent("ApiHealthCheck", attrs);

            if (!success) {
                NewRelic.noticeError("API unavailable, status=" + status);
                log.error("API health check FAILED status = " + status);
            } else {
                log.error("API health check OK status = " + status);
            }
        }
    }
}
