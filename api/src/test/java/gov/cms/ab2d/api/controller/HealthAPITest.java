package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.util.HealthCheck;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthAPITest {

    @Mock
    private HealthCheck healthCheck;

    @Mock
    private SQSEventClient sqsEventClient;

    @Mock
    private HttpServletRequest request;

    @Captor
    private ArgumentCaptor<LoggableEvent> captor;

    @Test
    void getHealth() {
        HealthAPI healthAPI = new HealthAPI(healthCheck, sqsEventClient);
        when(healthCheck.healthy()).thenReturn(true);
        assertEquals(HttpStatus.OK, healthAPI.getHealth(null).getStatusCode());

        when(healthCheck.healthy()).thenReturn(false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, healthAPI.getHealth(request).getStatusCode());
        verify(sqsEventClient, only()).sendLogs(captor.capture());
        ApiResponseEvent event = (ApiResponseEvent) captor.getValue();
        assertEquals(500, event.getResponseCode());
        assertEquals("API Health NOT Ok", event.getResponseString());
    }
}