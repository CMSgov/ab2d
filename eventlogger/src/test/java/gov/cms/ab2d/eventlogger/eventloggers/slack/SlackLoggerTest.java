package gov.cms.ab2d.eventlogger.eventloggers.slack;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import liquibase.util.StringUtils;
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

class SlackLoggerTest {

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

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), "testing");

        boolean result = slackLogger.logAlert("This is a special announcement");
        assertTrue(result, "");

        assertTrue(urls.containsAll(List.of("A", "B")));
        verify(slack, times(2)).send(anyString(), any(Payload.class));

        // Does payload contain the message and the environment sending the message
        assertTrue(webhookPayloads.get(0).toString().contains("This is a special announcement"));
        assertTrue(webhookPayloads.get(0).toString().contains("testing"));

        slackLogger.logTrace("This is a not so special announcement");
        assertTrue(urls.containsAll(List.of("C", "D")));
        verify(slack, times(4)).send(anyString(), any(Payload.class));

        assertTrue(webhookPayloads.get(2).toString().contains("This is a not so special announcement"));
    }

    @DisplayName("SlackLogger quietly handles Slack failure to process webhook")
    @Test
    void slackLoggerQuietlyHandlesBadResponseCodes() throws IOException {

        when(slack.send(anyString(), any(Payload.class)))
                .thenReturn(WebhookResponse.builder().code(400).build());

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), "testing");

        assertFalse(slackLogger.logAlert("oops"));
        assertFalse(slackLogger.logTrace("oops"));
    }

    @DisplayName("SlackLogger quietly handles failure to send webhook")
    @Test
    void slackLoggerQuietlyHandlesNetworkIssues() throws IOException {

        when(slack.send(anyString(), any(Payload.class))).thenThrow(IOException.class);

        SlackLogger slackLogger = new SlackLogger(slack, List.of("A", "B"), List.of("C", "D"), "testing");

        assertFalse(slackLogger.logAlert("oops"));
        assertFalse(slackLogger.logTrace("oops"));
    }

    @DisplayName("SlackLogger ignores default url 'na'")
    @Test
    void slackLoggerIgnoresDefaultUrl() throws IOException {

        SlackLogger slackLogger = new SlackLogger(slack, List.of("NA", "Na", "nA", "\t\tNA", "   NA\n"),
                List.of("NA", "Na", "nA", "\t\tNA", "   NA\n"), "testing");

        when(slack.send(anyString(), any(Payload.class))).thenThrow(IOException.class);

        assertTrue(slackLogger.logAlert("testing"));
        assertTrue(slackLogger.logTrace("testing"));

        verify(slack, times(0)).send(anyString(), any(Payload.class));
    }
}
