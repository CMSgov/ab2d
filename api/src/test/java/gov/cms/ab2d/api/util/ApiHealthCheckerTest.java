package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiHealthCheckerTest {

    @Mock
    private CloseableHttpClient mockClient;

    @Mock
    private Agent mockAgent;

    @Mock
    private Insights mockInsights;

    @Mock
    private HttpClientBuilder mockBuilder;

    private MockedStatic<HttpClients> httpClient;
    private MockedStatic<NewRelic> newRelic;

    private ApiHealthChecker checker;

    @BeforeEach
    void beforeEach() {
        httpClient = mockStatic(HttpClients.class);
        httpClient.when(HttpClients::custom).thenReturn(mockBuilder);
        when(mockBuilder.setDefaultRequestConfig(any(RequestConfig.class)))
                .thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockClient);
        newRelic = mockStatic(NewRelic.class);
        newRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
        when(mockAgent.getInsights()).thenReturn(mockInsights);

        checker = new ApiHealthChecker("ab2d-east-impl");
    }

    @AfterEach
    void afterEach() {
        httpClient.close();
        newRelic.close();
    }

    @Test
    void recordsSuccessAndNoErrorNoticedTest() throws Exception {
        when(mockClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenReturn(200);

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), argThat(map ->
                map.get("statusCode").equals(200) &&
                        map.get("success").equals(true)
        ));

        newRelic.verify(() -> NewRelic.noticeError(anyString()), never());

    }

    @Test
    void recordsFailureAndNoticesErrorTest() throws Exception {
        when(mockClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenReturn(500);

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), argThat(map ->
                map.get("statusCode").equals(500) &&
                        map.get("success").equals(false)));

        newRelic.verify(() -> NewRelic.noticeError("API unavailable, status=500"), times(1));
    }

    @Test
    void recordsFailureAndNoticesExceptionTest() throws Exception {
        when(mockClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenThrow(new IOException("network down"));

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), argThat(map ->
                map.get("statusCode").equals(-1) &&
                        map.get("success").equals(false)));

        newRelic.verify(() -> NewRelic.noticeError(isA(IOException.class)), times(1));
    }

//    @Test
//    void environmentIsNotInListTest() {
//        checker = new ApiHealthChecker("local");
//        checker.checkHealth();
//
//        newRelic.verify(() -> NewRelic.noticeError(isA(IOException.class)), never());
//    }
}
