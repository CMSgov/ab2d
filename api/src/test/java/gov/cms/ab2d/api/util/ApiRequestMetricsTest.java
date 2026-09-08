package gov.cms.ab2d.api.util;

import com.timgroup.statsd.StatsDClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static gov.cms.ab2d.api.util.ApiRequestMetrics.CLIENT_VERSION;
import static gov.cms.ab2d.api.util.ApiRequestMetrics.ERROR_COUNT;
import static gov.cms.ab2d.api.util.ApiRequestMetrics.REQUEST_COUNT;
import static gov.cms.ab2d.api.util.ApiRequestMetrics.REQUEST_DURATION;
import static gov.cms.ab2d.api.util.ApiRequestMetrics.REQUEST_SIZE;
import static gov.cms.ab2d.api.util.ApiRequestMetrics.RESPONSE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ApiRequestMetricsTest {

    private static final String ENV_TAG = "environment:" + Ab2dEnvironment.IMPL.getName();

    @Mock
    private StatsDClient statsDClient;

    private ApiRequestMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new ApiRequestMetrics(Ab2dEnvironment.IMPL.getName(), statsDClient);
    }

    @Test
    void successfulRequestEmitsCountDurationAndSizes() {
        metrics.recordRequest("/api/v2/fhir/Patient/$export", "GET", 202, 42, 0, 350, null);

        String[] tags = {ENV_TAG, "endpoint:/api/v2/fhir/patient/export", "method:get",
                "status_code:202", "status_class:2xx"};

        verify(statsDClient).increment(REQUEST_COUNT, tags);
        verify(statsDClient).histogram(REQUEST_DURATION, 42L, tags);
        verify(statsDClient).histogram(REQUEST_SIZE, 0L, tags);
        verify(statsDClient).histogram(RESPONSE_SIZE, 350L, tags);
        verify(statsDClient, never()).increment(eq(ERROR_COUNT), any(String[].class));
    }

    @Test
    void unknownSizesAreNotReported() {
        metrics.recordRequest("/health", "GET", 200, 1, -1, -1, null);

        String[] tags = {ENV_TAG, "endpoint:/health", "method:get", "status_code:200", "status_class:2xx"};

        verify(statsDClient).increment(REQUEST_COUNT, tags);
        verify(statsDClient).histogram(REQUEST_DURATION, 1L, tags);
        verifyNoMoreInteractions(statsDClient);
    }

    @Test
    void clientErrorEmitsErrorCountTaggedWithExceptionType() {
        metrics.recordRequest("/api/v1/fhir/Job/{jobUuid}/$status", "GET", 400, 5, -1, 120,
                "InvalidClientInputException");

        verify(statsDClient).increment(ERROR_COUNT, ENV_TAG, "endpoint:/api/v1/fhir/job/jobuuid/status",
                "method:get", "status_code:400", "status_class:4xx", "error_type:invalidclientinputexception");
    }

    @Test
    void serverErrorWithoutAnExceptionFallsBackToTheStatus() {
        metrics.recordRequest("/api/v1/fhir/Patient/$export", "POST", 503, 5, -1, -1, null);

        verify(statsDClient).increment(ERROR_COUNT, ENV_TAG, "endpoint:/api/v1/fhir/patient/export",
                "method:post", "status_code:503", "status_class:5xx", "error_type:http_503");
    }

    @Test
    void clientVersionIsReportedSeparatelyFromTheRequestTags() {
        metrics.recordClientVersion("v2", "AB2D-Client/1.4.0");

        verify(statsDClient).increment(CLIENT_VERSION, ENV_TAG, "api_version:v2",
                "client_version:ab2d-client/1.4.0");
    }

    @Test
    void missingClientVersionIsTaggedUnknown() {
        metrics.recordClientVersion(null, null);

        verify(statsDClient).increment(CLIENT_VERSION, ENV_TAG, "api_version:unknown",
                "client_version:unknown");
    }

    @Test
    void statusClassGroupsByHundreds() {
        assertEquals("2xx", ApiRequestMetrics.statusClass(204));
        assertEquals("3xx", ApiRequestMetrics.statusClass(302));
        assertEquals("4xx", ApiRequestMetrics.statusClass(429));
        assertEquals("5xx", ApiRequestMetrics.statusClass(500));
        assertEquals(ApiRequestMetrics.UNKNOWN, ApiRequestMetrics.statusClass(0));
        assertEquals(ApiRequestMetrics.UNKNOWN, ApiRequestMetrics.statusClass(600));
    }
}
