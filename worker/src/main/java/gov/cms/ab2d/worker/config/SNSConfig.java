package gov.cms.ab2d.worker.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static gov.cms.ab2d.worker.config.LocalstackConfig.configureBuilder;

@Configuration
@Slf4j
public class SNSConfig {

    @Bean
    public AmazonSNSAsync amazonSNSAsync() {
        return (AmazonSNSAsync) getSns(AmazonSNSAsyncClientBuilder
                .standard());
    }

    @Primary
    @Bean
    public AmazonSNS amazonSNS() {
        return getSns(configureBuilder(AmazonSNSClientBuilder
                .standard()));
    }

    @Bean
    public NotificationMessagingTemplate notificationMessagingTemplate(
            AmazonSNS amazonSNS) {
        return new NotificationMessagingTemplate(amazonSNS);
    }

    private AmazonSNS getSns(AwsClientBuilder<?, ?> builder) {
        return (AmazonSNS) builder.build();
    }

}
