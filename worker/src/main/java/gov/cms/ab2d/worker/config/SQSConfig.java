package gov.cms.ab2d.worker.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.support.NotificationMessageArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.List;

@Configuration
@Slf4j
public class SQSConfig {

    private final AWSStaticCredentialsProvider credentials =
            new AWSStaticCredentialsProvider(new BasicAWSCredentials("a", ""));
    private final String region = Regions.US_EAST_1.getName();

    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        return (AmazonSQSAsync) getSQs(AmazonSQSAsyncClientBuilder.standard());
    }

    @Primary
    @Bean
    public AmazonSQS amazonSQS() {
        return getSQs(AmazonSQSClientBuilder.standard());
    }

    private AmazonSQS getSQs(AwsClientBuilder<?, ?> builder) {
        String localstackUrl = System.getProperty("LOCALSTACK_URL");
        log.info("LOCALSTACK_URL: " + localstackUrl);
        if (null != localstackUrl) {
            builder.withEndpointConfiguration(getEndpointConfig(localstackUrl))
                    .withCredentials(credentials);
        }
        return (AmazonSQS) builder.build();
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(MessageConverter messageConverter) {
        var factory = new QueueMessageHandlerFactory();
        factory.setArgumentResolvers(List.of(new NotificationMessageArgumentResolver(messageConverter)));
        return factory;
    }

    @Bean
    protected MessageConverter messageConverter() {
        var converter = new MappingJackson2MessageConverter();
        converter.setSerializedPayloadClass(String.class);
        converter.setStrictContentTypeMatch(false);
        return converter;
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfig(String localstackURl) {
        return new AwsClientBuilder.EndpointConfiguration(localstackURl, region);
    }

}