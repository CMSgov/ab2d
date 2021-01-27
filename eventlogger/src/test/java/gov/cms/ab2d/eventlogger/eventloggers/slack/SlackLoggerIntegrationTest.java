package gov.cms.ab2d.eventlogger.eventloggers.slack;


import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class, properties="slack.alert.webhooks=A,B,slack.trace.webhooks=    ")
@Testcontainers
class SlackLoggerIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private SlackLogger slackLogger;

    @Test
    void areWebhooksProcessedCorrectly() {
        List<String> alertWebhooks = (List<String>) ReflectionTestUtils.getField(slackLogger, "slackAlertWebhooks");

        // Handle list of webhooks
        assertNotNull(alertWebhooks);
        assertEquals(2, alertWebhooks.size());
        assertTrue(alertWebhooks.containsAll(List.of("A", "B")));

        // Do not attempt to use empty webhooks
        List<String> traceWebhooks = (List<String>) ReflectionTestUtils.getField(slackLogger, "slackTraceWebhooks");
        assertNotNull(traceWebhooks);
        assertTrue(traceWebhooks.isEmpty());
    }
}
