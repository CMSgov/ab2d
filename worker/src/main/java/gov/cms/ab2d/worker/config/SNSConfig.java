package gov.cms.ab2d.worker.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "LOCALSTACK_URL")
public class SNSConfig {

    @Bean
    public AmazonSNSAsync amazonSNSAsync() {
        return (AmazonSNSAsync) getSns(AmazonSNSAsyncClientBuilder
                .standard());
    }

    @Primary
    @Bean
    public AmazonSNS amazonSNS() {
        return getSns(AmazonSNSClientBuilder
                .standard());
    }

    @Bean
    public NotificationMessagingTemplate notificationMessagingTemplate(
            AmazonSNS amazonSNS) {
        return new NotificationMessagingTemplate(amazonSNS);
    }

    private AmazonSNS getSns(AwsClientBuilder<?, ?> builder) {
        String localstackUrl = System.getProperty("LOCALSTACK_URL");
        log.info("LOCALSTACK_URL: " + localstackUrl);
        if (null != localstackUrl) {
            builder
                    .withEndpointConfiguration(getEndpointConfig(localstackUrl))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("a", "")));
        }

        return (AmazonSNS) builder
                .build();
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfig(String localstackURl) {
        return new AwsClientBuilder.EndpointConfiguration(localstackURl, Regions.US_EAST_1.getName());
    }

}
