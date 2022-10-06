package gov.cms.ab2d.common.health;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SlackAvailable {

    private final SlackLogger slackLogger;

    public boolean slackAvailable(String message) {
        return slackLogger.logTrace(message, Ab2dEnvironment.ALL);
    }
}
