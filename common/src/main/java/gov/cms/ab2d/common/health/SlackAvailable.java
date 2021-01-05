package gov.cms.ab2d.common.health;

import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlackAvailable {
    @Autowired
    private SlackLogger slackLogger;

    public boolean slackAvailable(String message) {
        return slackLogger.logTestMsg(message);
    }
}
