package gov.cms.ab2d.eventlogger.eventloggers.slack;

import com.slack.api.Slack;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class SlackLogger {

    private final Slack slack;

    private final List<String> slackAlertWebhooks;

    private final List<String> slackTraceWebhooks;

    private final String appEnv;

    public SlackLogger(Slack slackClient, @Value("${slack.alert.webhooks}") List<String> slackAlertWebhooks,
                       @Value("${slack.trace.webhooks}") List<String> slackTraceWebhooks,
                       @Value("${execution.env}") String appEnv) {
        this.slack = slackClient;
        this.slackAlertWebhooks = slackAlertWebhooks.stream().filter(StringUtils::isNotBlank)
                .map(String::trim).collect(toList());
        this.slackTraceWebhooks = slackTraceWebhooks.stream().filter(StringUtils::isNotBlank)
                .map(String::trim).collect(toList());
        this.appEnv = appEnv;
    }

    /**
     * Log a message to slack. You can use markdown format in the message. See:
     * https://api.slack.com/reference/surfaces/formatting for a reference.
     *
     * Alerts are meant for the entire team and indicate an event that needs
     * to be tracked or handled immediately.
     *
     * @param message message to log
     * @return true if client successfully logged message
     */
    public boolean logAlert(String message) {
        return log(message, slack, appEnv, slackAlertWebhooks);
    }

    /**
     * Log a message to slack. You can use markdown format in the message. See:
     * https://api.slack.com/reference/surfaces/formatting for a reference.
     *
     * Traces are meant for developers.
     *
     * @param message message to log
     * @return true if all webhooks successfully received message
     */
    public boolean logTrace(String message) {
        return log(message, slack, appEnv, slackTraceWebhooks);
    }

    static boolean log(String msg, Slack slack, String env, List<String> webhooks) {
        try {

            SectionBlock sectionBlock = new SectionBlock();
            sectionBlock.setBlockId(UUID.randomUUID() + "blockId");

            MarkdownTextObject textObject = new MarkdownTextObject();
            textObject.setText(msg + "\n\n" + env);
            sectionBlock.setText(textObject);

            Payload payload = Payload.builder()
                    .blocks(singletonList(sectionBlock))
                    .build();

            boolean successfullyDelivered = true;
            for (String webhook : webhooks) {
                WebhookResponse response = slack.send(webhook, payload);
                if (response.getCode() != HttpStatus.OK.value()) {
                    log.error("unable to log to slack {} - {}", response.getCode(), response.getMessage());
                    successfullyDelivered = false;
                }
            }

            // At least one webhook was not successfully used
            if (!successfullyDelivered) {
                log.error("unable to log to slack - " + msg);
            }

            return successfullyDelivered;
        } catch (Exception ex) {
            log.error("Unable to log to slack - " + msg, ex);
            return false;
        }
    }
}
