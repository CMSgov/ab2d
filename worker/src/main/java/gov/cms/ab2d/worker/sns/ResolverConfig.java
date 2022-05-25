package gov.cms.ab2d.worker.sns;

import com.amazonaws.services.sns.AmazonSNS;
import org.springframework.cloud.aws.messaging.endpoint.NotificationMessageHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.endpoint.NotificationSubjectHandlerMethodArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class ResolverConfig {

    @Bean
    public NotificationMessageHandlerMethodArgumentResolver notificationMessageHandlerMethodArgumentResolver() {
        return new NotificationMessageHandlerMethodArgumentResolver();
    }

    @Bean
    public NotificationSubjectHandlerMethodArgumentResolver notificationSubjectHandlerMethodArgumentResolver() {
        return new NotificationSubjectHandlerMethodArgumentResolver();
    }

    @Bean
    public NotificationStatusHandlerMethodArgumentResolver notificationStatusHandlerMethodArgumentResolver(AmazonSNS amazonSNS) {
        return new NotificationStatusHandlerMethodArgumentResolver(amazonSNS);
    }


}