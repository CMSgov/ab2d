package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApiHealthCheckerTest {

    private CloseableHttpClient mockClient;
    private Insights mockInsights;
    private ArgumentCaptor<Map<String, Object>> attrsCaptor;

    private ApiHealthChecker checker;
    MockedStatic<HttpClients> httpClients;
    MockedStatic<NewRelic> newRelic;

    @BeforeEach
    void beforeEach() {
        mockClient = mock(CloseableHttpClient.class);
        Agent mockAgent = mock(Agent.class);
        mockInsights = mock(Insights.class);
        attrsCaptor = ArgumentCaptor.forClass(Map.class);

        when(mockAgent.getInsights()).thenReturn(mockInsights);

        checker = new ApiHealthChecker("ab2d-east-impl");
        httpClients = mockStatic(HttpClients.class);
        newRelic = mockStatic(NewRelic.class);

        httpClients.when(HttpClients::createDefault).thenReturn(mockClient);
        newRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
    }

    @AfterEach
    void afterEach() {
        httpClients.close();
        newRelic.close();
    }

    @Test
    void recordsSuccessAndNoErrorNoticedTest() throws Exception {
        when(mockClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenReturn(200);

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), attrsCaptor.capture());
        Map<String, Object> evt = attrsCaptor.getValue();
        assertEquals(200, evt.get("statusCode"));
        assertTrue((Boolean) evt.get("success"));

        newRelic.verify(() -> NewRelic.noticeError(startsWith("API unavailable")), never());

    }

    @Test
    void recordsFailureAndNoticesErrorTest() throws Exception {
        when(mockClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenReturn(500);

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), attrsCaptor.capture());
        Map<String, Object> evt = attrsCaptor.getValue();
        assertEquals(500, evt.get("statusCode"));
        assertFalse((Boolean) evt.get("success"));

        newRelic.verify(() -> NewRelic.noticeError("API unavailable, status=500"), times(1));
    }

    @Test
    void recordsFailureAndNoticesExceptionTest() throws Exception {
        when(mockClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenThrow(new IOException("network down"));

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), attrsCaptor.capture());
        Map<String, Object> evt = attrsCaptor.getValue();
        assertEquals(-1, evt.get("statusCode"));
        assertFalse((Boolean) evt.get("success"));

        newRelic.verify(() -> NewRelic.noticeError(isA(IOException.class)), times(1));
    }

    @Test
    void environmentIsNotInListTest() {
        checker = new ApiHealthChecker("local");
        checker.checkHealth();

        newRelic.verify(() -> NewRelic.noticeError(isA(IOException.class)), never());
    }
}
