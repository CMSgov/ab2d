package gov.cms.ab2d.api.config;

import gov.cms.ab2d.api.util.ApiRequestMetrics;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApiRequestMetricsFilterTest {

    private static final String FILTERS = "^/health$,^/akamai-test-object.html$";

    @Mock
    private ApiRequestMetrics metrics;

    private ApiRequestMetricsFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiRequestMetricsFilter(metrics, FILTERS);
        filter.constructFilters();
    }

    @Test
    void recordsMatchedRequestMappingRatherThanTheRawUri() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/v2/fhir/Job/1cb4dd8d-9ad8-4b2b-bd54-e1e35e1c5b7d/$status");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v2/fhir/Job/{jobUuid}/$status");
        request.addHeader("User-Agent", "AB2D-Client/1.4.0 (Linux; x86_64)");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, writingChain("done"));

        verify(metrics).recordRequest(eq("/api/v2/fhir/Job/{jobUuid}/$status"), eq("GET"), eq(200),
                anyLong(), eq(-1L), eq(4L), eq(null));
        verify(metrics).recordClientVersion("v2", "AB2D-Client/1.4.0");
    }

    @Test
    void collapsesJobUuidsWhenTheRequestNeverReachedAHandler() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/v1/fhir/Job/1cb4dd8d-9ad8-4b2b-bd54-e1e35e1c5b7d/$status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        filter.doFilter(request, response, new MockFilterChain());

        verify(metrics).recordRequest(eq("/api/v1/fhir/Job/{id}/$status"), eq("GET"), eq(401),
                anyLong(), anyLong(), anyLong(), eq(null));
    }

    @Test
    void collapsesUnmatchedUrisIntoASingleEndpointTag() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wp-login.php");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        filter.doFilter(request, response, new MockFilterChain());

        verify(metrics).recordRequest(eq("unmatched"), eq("GET"), eq(404), anyLong(), anyLong(), anyLong(),
                eq(null));
        verify(metrics).recordClientVersion("none", null);
    }

    @Test
    void tagsErrorsWithTheExceptionRecordedByTheExceptionHandlers() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/fhir/Patient/$export");
        request.setAttribute(ApiRequestMetrics.ERROR_TYPE_ATTRIBUTE, "BadJWTTokenException");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        filter.doFilter(request, response, new MockFilterChain());

        verify(metrics).recordRequest(anyString(), eq("GET"), eq(403), anyLong(), anyLong(), anyLong(),
                eq("BadJWTTokenException"));
    }

    @Test
    void tagsErrorsWithTheExceptionResolvedByTheDispatcherServlet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/fhir/Patient/$export");
        request.setAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE, new IllegalStateException("boom"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        filter.doFilter(request, response, new MockFilterChain());

        verify(metrics).recordRequest(anyString(), eq("GET"), eq(500), anyLong(), anyLong(), anyLong(),
                eq("IllegalStateException"));
    }

    @Test
    void reportsAnEscapingExceptionAsAServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v3/fhir/Patient/$export");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failing = (req, res) -> {
            throw new IllegalArgumentException("boom");
        };

        assertThrows(IllegalArgumentException.class, () -> filter.doFilter(request, response, failing));

        verify(metrics).recordRequest(anyString(), eq("POST"), eq(500), anyLong(), anyLong(), anyLong(),
                eq("IllegalArgumentException"));
    }

    @Test
    void skipsExcludedUris() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(metrics);
    }

    @Test
    void measuresEveryUriWhenNoFiltersAreConfigured() throws Exception {
        ApiRequestMetricsFilter unfiltered = new ApiRequestMetricsFilter(metrics, "  ");
        unfiltered.constructFilters();

        unfiltered.doFilter(new MockHttpServletRequest("GET", "/health"), new MockHttpServletResponse(),
                new MockFilterChain());

        verify(metrics).recordRequest(eq("/health"), eq("GET"), anyInt(), anyLong(), anyLong(), anyLong(),
                eq(null));
    }

    @Test
    void aFailureToReportMetricsDoesNotFailTheRequest() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("statsd down"))
                .when(metrics).recordRequest(anyString(), anyString(), anyInt(), anyLong(), anyLong(), anyLong(),
                        org.mockito.ArgumentMatchers.isNull());

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/fhir/Patient/$export"), response,
                new MockFilterChain());

        assertEquals(200, response.getStatus());
        verify(metrics, never()).recordClientVersion(anyString(), anyString());
    }

    @Test
    void normalizeUriCollapsesVariableSegments() {
        assertEquals("/api/v1/fhir/Job/{id}/$status",
                ApiRequestMetricsFilter.normalizeUri("/api/v1/fhir/Job/1cb4dd8d-9ad8-4b2b-bd54-e1e35e1c5b7d/$status"));
        assertEquals("/api/v2/fhir/Job/{id}/file/{id}",
                ApiRequestMetricsFilter.normalizeUri("/api/v2/fhir/Job/12/file/Z0000_0001.ndjson.gz"));
        assertEquals("/", ApiRequestMetricsFilter.normalizeUri("/"));
        assertEquals("unmatched", ApiRequestMetricsFilter.normalizeUri(null));
    }

    private static FilterChain writingChain(String body) {
        return (request, response) -> {
            try {
                response.getOutputStream().write(body.getBytes());
            } catch (IOException e) {
                throw new ServletException(e);
            }
        };
    }
}
