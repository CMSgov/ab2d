package gov.cms.ab2d.api.util;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiHealthCheckerTest {
    @Mock
    private Agent mockAgent;
    @Mock
    private Insights mockInsights;

    private MockedStatic<NewRelic> newRelicStatic;

    @BeforeEach
    void setUp() {
        newRelicStatic = mockStatic(NewRelic.class);
        newRelicStatic.when(NewRelic::getAgent).thenReturn(mockAgent);
        when(mockAgent.getInsights()).thenReturn(mockInsights);
    }

    @AfterEach
    void tearDown() {
        newRelicStatic.close();
    }

    @Test
    void checkHealthTest() {
        ApiHealthChecker checker = new ApiHealthChecker("ab2d-east-impl");
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        checker.checkHealth();

        verify(mockInsights).recordCustomEvent(eq("ApiHealthCheck"), captor.capture());
        Map<String, Object> attrs = captor.getValue();
        assertEquals(Ab2dEnvironment.IMPL, attrs.get("environment"));
        assertEquals("success", attrs.get("success"));
    }
}
