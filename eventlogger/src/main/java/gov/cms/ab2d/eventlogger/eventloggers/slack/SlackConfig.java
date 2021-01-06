package gov.cms.ab2d.eventlogger.eventloggers.slack;

import com.slack.api.Slack;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {
    @Bean
    public static Slack slackClient() {
        return Slack.getInstance();
    }
}
