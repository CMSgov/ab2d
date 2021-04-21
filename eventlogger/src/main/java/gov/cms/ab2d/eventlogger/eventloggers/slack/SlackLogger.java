package gov.cms.ab2d.eventlogger.eventloggers.slack;

import com.slack.api.Slack;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
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

    private final Ab2dEnvironment ab2dEnvironment;

    public SlackLogger(Slack slackClient, @Value("${slack.alert.webhooks}") List<String> slackAlertWebhooks,
                       @Value("${slack.trace.webhooks}") List<String> slackTraceWebhooks,
                       Ab2dEnvironment ab2dEnvironment) {
        this.slack = slackClient;
        this.slackAlertWebhooks = slackAlertWebhooks.stream().filter(StringUtils::isNotBlank)
                .map(String::trim).collect(toList());
        this.slackTraceWebhooks = slackTraceWebhooks.stream().filter(StringUtils::isNotBlank)
                .map(String::trim).collect(toList());
        this.ab2dEnvironment = ab2dEnvironment;
    }

    /**
     * Log a message to slack. You can use markdown format in the message. See:
     * https://api.slack.com/reference/surfaces/formatting for a reference.
     *
     * Alerts are meant for the entire team and indicate an event that needs
     * to be tracked or handled immediately.
     *
     * @param message alert to log
     * @return true if client successfully logged message
     */
    public boolean logAlert(String message, List<Ab2dEnvironment> ab2dEnvironments) {
        if (ab2dEnvironments != null && ab2dEnvironments.contains(ab2dEnvironment)) {
            return log(message, slack, ab2dEnvironment, slackAlertWebhooks);
        }

        return false;
    }

    /**
     * Log a message to slack. You can use markdown format in the message. See:
     * https://api.slack.com/reference/surfaces/formatting for a reference.
     *
     * Traces are meant for developers.
     *
     * @param message trace to log
     * @return true if all webhooks successfully received message
     */
    public boolean logTrace(String message, List<Ab2dEnvironment> ab2dEnvironments) {
        if (ab2dEnvironments != null && ab2dEnvironments.contains(ab2dEnvironment)) {
            return log(message, slack, ab2dEnvironment, slackTraceWebhooks);
        }

        return false;
    }

    static boolean log(String msg, Slack slack, Ab2dEnvironment executionEnv, List<String> webhooks) {
        try {

            SectionBlock sectionBlock = new SectionBlock();
            sectionBlock.setBlockId(UUID.randomUUID() + "blockId");

            MarkdownTextObject textObject = new MarkdownTextObject();
            textObject.setText(msg + "\n\n" + executionEnv.getName());
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
