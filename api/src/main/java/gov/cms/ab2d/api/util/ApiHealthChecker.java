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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
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

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth() {
        if (healthUrl != null && !healthUrl.isEmpty()) {

            boolean success = false;
            int status = -1;

            try (CloseableHttpClient client = HttpClients.createDefault()) {
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
                log.error("API health check FAILED status = " +  status);
            } else {
                log.error("API health check OK status = " + status);
            }
        }
    }

    @Scheduled(fixedRateString = "300000")  // 5 minutes
    public void checkHealth2() {
        if (healthUrl != null && !healthUrl.isEmpty()) {

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .GET()
                    .build();

            java.net.http.HttpResponse<Void> resp = null;
            try {
                resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.discarding());
            } catch (IOException | InterruptedException e) {
                log.error("Health check failed for URL " + healthUrl + " — see stack trace:" + e);
            }
            int status = resp.statusCode();
            log.error("API health check status = " + status);
        }
    }
}
