package gov.cms.ab2d.api.config;

import gov.cms.ab2d.api.util.ApiRequestMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V3;

/**
 * Times every request that reaches the API and hands the result to {@link ApiRequestMetrics}, which
 * turns it into the Datadog custom metrics for request counts, latency, errors, client versions and
 * payload sizes.
 *
 * <p>Ordered ahead of the Spring Security filter chain so authentication and authorization failures
 * (401/403), which never reach a controller, are counted like any other response.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiRequestMetricsFilter extends OncePerRequestFilter {

    /** Matches the leading product token of a User-Agent, for example {@code curl/8.7.1}. */
    private static final Pattern USER_AGENT_PRODUCT = Pattern.compile("^\\s*([^\\s;(,]+)");

    /** Segments of an unmatched URI that vary per request and must not become their own tag value. */
    private static final Pattern VARIABLE_SEGMENT =
            Pattern.compile("(?i)^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|\\d+|\\S*\\.ndjson(\\.gz)?)$");

    private static final int MAX_CLIENT_VERSION_LENGTH = 40;
    private static final String NO_API_VERSION = "none";

    /**
     * Endpoint reported for requests that never matched a request mapping. Unmatched URIs are
     * attacker- or bot-controlled, so they are collapsed into one tag value rather than becoming an
     * unbounded number of custom metric time series.
     */
    private static final String UNMATCHED_ENDPOINT = "unmatched";

    private final ApiRequestMetrics metrics;
    private final String uriFilters;

    // If predicate.test("uri") -> true the URI does not match any regex filter and should be measured
    private Predicate<String> uriFilter;

    public ApiRequestMetricsFilter(ApiRequestMetrics metrics,
                                   @Value("${api.metrics.filter:#{null}}") String uriFilters) {
        this.metrics = metrics;
        this.uriFilters = uriFilters;
    }

    @PostConstruct
    void constructFilters() {
        if (StringUtils.isBlank(uriFilters)) {
            uriFilter = uri -> true;
            return;
        }

        List<Predicate<String>> compiledFilters = List.of(uriFilters.split(",")).stream()
                .filter(StringUtils::isNotBlank)
                .map(Pattern::compile).map(Pattern::asPredicate)
                .toList();

        uriFilter = compiledFilters.stream().reduce(Predicate::or).orElse(uri -> false).negate();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !uriFilter.test(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long startNanos = System.nanoTime();
        CountingHttpServletResponseWrapper countingResponse = new CountingHttpServletResponseWrapper(response);
        String thrown = null;

        try {
            chain.doFilter(request, countingResponse);
        } catch (IOException | ServletException | RuntimeException e) {
            thrown = e.getClass().getSimpleName();
            throw e;
        } finally {
            recordRequest(request, countingResponse, startNanos, thrown);
        }
    }

    private void recordRequest(HttpServletRequest request, CountingHttpServletResponseWrapper response,
                               long startNanos, String thrown) {
        try {
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;

            // An exception escaping the filter chain is turned into a 500 by the container after this
            // filter returns, so the status on the response does not reflect it yet.
            int statusCode = thrown != null && response.getStatus() < 400
                    ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    : response.getStatus();

            metrics.recordRequest(endpoint(request, statusCode), request.getMethod(), statusCode, durationMillis,
                    request.getContentLengthLong(), response.getBytesWritten(), errorType(request, thrown));
            metrics.recordClientVersion(apiVersion(request.getRequestURI()), clientVersion(request));
        } catch (Exception e) {
            // Metrics must never break a request that otherwise succeeded
            log.warn("Could not report API request metrics", e);
        }
    }

    /**
     * Report the matched request mapping (for example {@code /api/v2/fhir/Job/{jobUuid}/$status})
     * rather than the raw URI, which embeds job UUIDs and file names and would create a new custom
     * metric time series per request.
     */
    private static String endpoint(HttpServletRequest request, int statusCode) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return pattern.toString();
        }
        // Requests rejected before handler mapping (401/403) never got a pattern, so derive one
        return statusCode == HttpServletResponse.SC_NOT_FOUND
                ? UNMATCHED_ENDPOINT
                : normalizeUri(request.getRequestURI());
    }

    /**
     * Collapse the job UUIDs and file names in a raw URI down to placeholders so an unauthenticated
     * caller cannot create a new time series per request.
     */
    static String normalizeUri(String uri) {
        if (StringUtils.isBlank(uri)) {
            return UNMATCHED_ENDPOINT;
        }
        StringBuilder normalized = new StringBuilder();
        for (String segment : uri.split("/", -1)) {
            if (segment.isEmpty()) {
                continue;
            }
            normalized.append('/')
                    .append(VARIABLE_SEGMENT.matcher(segment).matches() ? "{id}" : segment);
        }
        return normalized.isEmpty() ? "/" : normalized.toString();
    }

    /**
     * Exceptions handled by {@code ErrorHandler} never reach this filter. Spring's DispatcherServlet
     * records them on the request once the exception resolver produced a body-only response, and
     * {@code FilterChainExceptionHandler} does the same for the security filter chain.
     */
    private static String errorType(HttpServletRequest request, String thrown) {
        Object recorded = request.getAttribute(ApiRequestMetrics.ERROR_TYPE_ATTRIBUTE);
        if (recorded != null) {
            return recorded.toString();
        }
        Object resolved = request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
        if (resolved == null) {
            resolved = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        }
        if (resolved instanceof Throwable throwable) {
            return throwable.getClass().getSimpleName();
        }
        return thrown;
    }

    private static String apiVersion(String uri) {
        if (uri == null) {
            return NO_API_VERSION;
        }
        if (uri.startsWith(API_PREFIX_V1)) {
            return "v1";
        }
        if (uri.startsWith(API_PREFIX_V2)) {
            return "v2";
        }
        if (uri.startsWith(API_PREFIX_V3)) {
            return "v3";
        }
        return NO_API_VERSION;
    }

    /**
     * Reduce the User-Agent to its leading product token so the tag identifies the client software and
     * its version ({@code ab2d-client/1.4.0}) without carrying the platform details that follow it.
     */
    private static String clientVersion(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) {
            return null;
        }
        var matcher = USER_AGENT_PRODUCT.matcher(userAgent);
        String product = matcher.find() ? matcher.group(1) : userAgent.trim();
        return product.length() > MAX_CLIENT_VERSION_LENGTH
                ? product.substring(0, MAX_CLIENT_VERSION_LENGTH)
                : product;
    }
}
