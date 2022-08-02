package gov.cms.ab2d.eventlogger.eventloggers.slack;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SlackLoggerTest {

    private final Slack slack = mock(Slack.class);

    @DisplayName("SlackLogger iterates over list")
    @Test
    void slackLoggerSendsToAllWebhooksInList() throws IOException {

        List<String> urls = new ArrayList<>();
        List<Payload> webhookPayloads = new ArrayList<>();
        when(slack.send(anyString(), any(Payload.class))).thenAnswer((Answer<WebhookResponse>) invocation -> {

            assertNotNull(invocation.getArgument(0));
            assertFalse(StringUtils.isEmpty(invocation.getArgument(0)));

            urls.add(invocation.getArgument(0));
            webhookPayloads.add(invocation.getArgument(1));
            return WebhookResponse.builder().code(200).build();
        });

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), Ab2dEnvironment.LOCAL);

        boolean result = slackLogger.logAlert("This is a special announcement", Ab2dEnvironment.ALL);
        assertTrue(result, "");

        assertTrue(urls.containsAll(List.of("A", "B")));
        verify(slack, times(2)).send(anyString(), any(Payload.class));

        // Does payload contain the message and the environment sending the message
        assertTrue(webhookPayloads.get(0).toString().contains("This is a special announcement"));
        assertTrue(webhookPayloads.get(0).toString().contains("local"));

        slackLogger.logTrace("This is a not so special announcement", Ab2dEnvironment.ALL);
        assertTrue(urls.containsAll(List.of("C", "D")));
        verify(slack, times(4)).send(anyString(), any(Payload.class));

        assertTrue(webhookPayloads.get(2).toString().contains("This is a not so special announcement"));
    }

    @DisplayName("SlackLogger sends logging if correct execution environment")
    @Test
    void slackLoggerSendsIfEnvironment() throws IOException {

        List<String> urls = new ArrayList<>();
        List<Payload> webhookPayloads = new ArrayList<>();
        when(slack.send(anyString(), any(Payload.class))).thenAnswer((Answer<WebhookResponse>) invocation -> {

            assertNotNull(invocation.getArgument(0));
            assertFalse(StringUtils.isEmpty(invocation.getArgument(0)));

            urls.add(invocation.getArgument(0));
            webhookPayloads.add(invocation.getArgument(1));
            return WebhookResponse.builder().code(200).build();
        });

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), Ab2dEnvironment.PRODUCTION);

        // Send alert only in production environment
        boolean result = slackLogger.logAlert("This is a special announcement",
                List.of(Ab2dEnvironment.SANDBOX, Ab2dEnvironment.PRODUCTION));
        assertTrue(result, "");

        assertTrue(urls.containsAll(List.of("A", "B")));
        verify(slack, times(2)).send(anyString(), any(Payload.class));

        // Does payload contain the message and the environment sending the message
        assertTrue(webhookPayloads.get(0).toString().contains(Ab2dEnvironment.PRODUCTION.getName()));

        // Send trace only in production environment
        slackLogger.logTrace("This is a not so special announcement",
                List.of(Ab2dEnvironment.SANDBOX, Ab2dEnvironment.PRODUCTION));
        assertTrue(urls.containsAll(List.of("C", "D")));
        verify(slack, times(4)).send(anyString(), any(Payload.class));

        assertTrue(webhookPayloads.get(2).toString().contains(Ab2dEnvironment.PRODUCTION.getName()));
    }

    @DisplayName("SlackLogger does not send logging if incorrect execution environment")
    @Test
    void slackLoggerSendsIfNotEnvironment() throws IOException {

        List<String> urls = new ArrayList<>();
        List<Payload> webhookPayloads = new ArrayList<>();
        when(slack.send(anyString(), any(Payload.class))).thenAnswer((Answer<WebhookResponse>) invocation -> {

            assertNotNull(invocation.getArgument(0));
            assertFalse(StringUtils.isEmpty(invocation.getArgument(0)));

            urls.add(invocation.getArgument(0));
            webhookPayloads.add(invocation.getArgument(1));
            return WebhookResponse.builder().code(200).build();
        });

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), Ab2dEnvironment.PRODUCTION);

        // Send alert only in production environment
        boolean result = slackLogger.logAlert("This is a special announcement",
                List.of(Ab2dEnvironment.SANDBOX, Ab2dEnvironment.IMPL));
        assertFalse(result, "");
        verify(slack, never()).send(anyString(), any(Payload.class));

        // Send trace only in production environment
        slackLogger.logTrace("This is a not so special announcement",
                List.of(Ab2dEnvironment.SANDBOX, Ab2dEnvironment.IMPL));
        verify(slack, never()).send(anyString(), any(Payload.class));
    }

    @DisplayName("SlackLogger quietly handles Slack failure to process webhook")
    @Test
    void slackLoggerQuietlyHandlesBadResponseCodes() throws IOException {

        when(slack.send(anyString(), any(Payload.class)))
                .thenReturn(WebhookResponse.builder().code(400).build());

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"),  Ab2dEnvironment.LOCAL);

        assertFalse(slackLogger.logAlert("oops", Ab2dEnvironment.ALL));
        assertFalse(slackLogger.logTrace("oops", Ab2dEnvironment.ALL));
    }

    @DisplayName("SlackLogger quietly handles failure to send webhook")
    @Test
    void slackLoggerQuietlyHandlesNetworkIssues() throws IOException {

        when(slack.send(anyString(), any(Payload.class))).thenThrow(IOException.class);

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), Ab2dEnvironment.LOCAL);

        assertFalse(slackLogger.logAlert("oops", Ab2dEnvironment.ALL));
        assertFalse(slackLogger.logTrace("oops", Ab2dEnvironment.ALL));
    }
}
