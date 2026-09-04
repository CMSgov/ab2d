package gov.cms.ab2d.api.util;

import com.timgroup.statsd.StatsDClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Emits the AB2D API's Datadog custom metrics over DogStatsD. Every metric name here is relative to
 * the {@code ab2d} prefix configured on the {@link StatsDClient}, so
 * {@code api.request.count} is reported to Datadog as {@code ab2d.api.request.count} and shows up
 * under the "ab2d Custom Metrics" group of the AB2D dashboard.
 */
@Slf4j
@Component
public class ApiRequestMetrics {

    static final String REQUEST_COUNT = "api.request.count";
    static final String REQUEST_DURATION = "api.request.duration";
    static final String ERROR_COUNT = "api.error.count";
    static final String CLIENT_VERSION = "api.client.version";
    static final String REQUEST_SIZE = "api.request.size";
    static final String RESPONSE_SIZE = "api.response.size";

    static final String UNKNOWN = "unknown";

    /**
     * Request attribute used to hand this class the exception behind a failed request. Exceptions that
     * are turned into a response by {@code ErrorHandler} never propagate to the metrics filter, so the
     * code that resolves them records the type here instead.
     */
    public static final String ERROR_TYPE_ATTRIBUTE = "gov.cms.ab2d.api.metrics.ERROR_TYPE";

    /** Datadog silently rewrites anything outside of this set, so do the replacement up front. */
    private static final String DISALLOWED_TAG_CHARS = "[^a-z0-9._/-]";
    private static final int MAX_TAG_LENGTH = 100;

    private final String environmentTag;
    private final StatsDClient statsDClient;

    public ApiRequestMetrics(@Value("${execution.env}") String executionEnv, StatsDClient statsDClient) {
        this.environmentTag = "environment:" + Ab2dEnvironment.fromName(executionEnv).getName();
        this.statsDClient = statsDClient;
    }

    /**
     * Report a single completed request. Emits {@code api.request.count}, {@code api.request.duration}
     * and, when the sizes are known, {@code api.request.size} and {@code api.response.size}. Requests
     * that ended in a 4xx or 5xx additionally emit {@code api.error.count}.
     *
     * @param endpoint       matched request mapping, for example {@code api/v2/fhir/patient/export}
     * @param httpMethod     HTTP verb of the request
     * @param statusCode     HTTP status the client received
     * @param durationMillis wall clock time spent handling the request
     * @param requestSize    request body size in bytes, or a negative number when unknown
     * @param responseSize   response body size in bytes, or a negative number when unknown
     * @param errorType      exception that caused the failure, or null when the request threw nothing
     */
    public void recordRequest(String endpoint, String httpMethod, int statusCode, long durationMillis,
                              long requestSize, long responseSize, String errorType) {
        String[] tags = requestTags(endpoint, httpMethod, statusCode);

        statsDClient.increment(REQUEST_COUNT, tags);
        statsDClient.histogram(REQUEST_DURATION, durationMillis, tags);

        if (requestSize >= 0) {
            statsDClient.histogram(REQUEST_SIZE, requestSize, tags);
        }
        if (responseSize >= 0) {
            statsDClient.histogram(RESPONSE_SIZE, responseSize, tags);
        }

        if (statusCode >= 400) {
            List<String> errorTags = new ArrayList<>(List.of(tags));
            errorTags.add("error_type:" + sanitize(errorType(statusCode, errorType)));
            statsDClient.increment(ERROR_COUNT, errorTags.toArray(new String[0]));
        }
    }

    /**
     * Report which flavor of the API a client is calling and what client software it is calling with.
     * Kept separate from {@link #recordRequest} so the (comparatively noisy) client version tag does
     * not multiply the cardinality of the request and error counts.
     *
     * @param apiVersion    API version segment of the URI, for example {@code v2}
     * @param clientVersion leading User-Agent product token, for example {@code curl/8.7.1}
     */
    public void recordClientVersion(String apiVersion, String clientVersion) {
        statsDClient.increment(CLIENT_VERSION,
                environmentTag,
                "api_version:" + sanitize(apiVersion),
                "client_version:" + sanitize(clientVersion));
    }

    private String[] requestTags(String endpoint, String httpMethod, int statusCode) {
        return new String[]{
                environmentTag,
                "endpoint:" + sanitize(endpoint),
                "method:" + sanitize(httpMethod),
                "status_code:" + statusCode,
                "status_class:" + statusClass(statusCode)
        };
    }

    static String statusClass(int statusCode) {
        if (statusCode < 100 || statusCode > 599) {
            return UNKNOWN;
        }
        return (statusCode / 100) + "xx";
    }

    /**
     * Prefer the exception that actually failed the request. Errors raised by the servlet container or
     * by Spring Security never surface an exception to the filter, so fall back to the status itself.
     */
    private static String errorType(int statusCode, String errorType) {
        return errorType == null || errorType.isBlank() ? "http_" + statusCode : errorType;
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String cleaned = value.toLowerCase(Locale.ROOT)
                // '$' marks a FHIR operation and '{}' a path variable; both read fine without them
                .replaceAll("[{}$]", "")
                .replaceAll(DISALLOWED_TAG_CHARS, "_");
        if (cleaned.length() > MAX_TAG_LENGTH) {
            cleaned = cleaned.substring(0, MAX_TAG_LENGTH);
        }
        return cleaned.isBlank() ? UNKNOWN : cleaned;
    }
}
