package gov.cms.ab2d.eventlogger.eventloggers.slack;

import com.slack.api.Slack;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
public class SlackLogger {
    @Value("${logger.slack.hpms.url}")
    private String slackHPMSUrl;

    @Value("${logger.slack.error.url}")
    private String slackErrorUrl;

    @Value("${logger.slack.test.url}")
    private String slackTestUrl;

    @Value("${execution.env}")
    private String appEnv;

    @Autowired
    private Slack slack;

    /**
     * Log a message to slack. You can use markdown format in the message. See:
     * https://api.slack.com/reference/surfaces/formatting for a reference.
     *
     * @param message
     * @return
     */
    public boolean logHpmsMsg(String message) {
        return log(message, slack, appEnv, slackHPMSUrl);
    }

    public boolean logErrorMsg(String message) {
        return log(message, slack, appEnv, slackErrorUrl);
    }

    public boolean logTestMsg(String message) {
        return log(message, slack, appEnv, slackTestUrl);
    }

    static boolean log(String msg, Slack slack, String env, String slackUrl) {
        try {
            SectionBlock sectionBlock = new SectionBlock();
            sectionBlock.setBlockId(UUID.randomUUID() + "blockId");
            MarkdownTextObject textObject = new MarkdownTextObject();
            textObject.setText(msg + "\n\n" + env);
            sectionBlock.setText(textObject);
            Payload payload = Payload.builder()
                    .blocks(Collections.singletonList(sectionBlock))
                    .build();
            WebhookResponse response = slack.send(slackUrl, payload);
            if (response.getCode() == HttpStatus.OK.value()) {
                return true;
            }
            log.error("Unable to log to slack - " + msg + "/" + response.getCode());
            return false;
        } catch (Exception ex) {
            log.error("Unable to log to slack - " + msg, ex);
            return false;
        }
    }
}
