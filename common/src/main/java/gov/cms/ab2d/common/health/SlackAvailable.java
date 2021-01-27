package gov.cms.ab2d.common.health;

import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import org.springframework.stereotype.Component;

@Component
public class SlackAvailable {

    private final SlackLogger slackLogger;

    public SlackAvailable(SlackLogger slackLogger) {
        this.slackLogger = slackLogger;
    }

    public boolean slackAvailable(String message) {
        return slackLogger.logTrace(message);
    }
}
