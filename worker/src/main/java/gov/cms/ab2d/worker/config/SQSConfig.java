package gov.cms.ab2d.worker.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.support.NotificationMessageArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.List;

@Configuration
public class SQSConfig {

    private final AWSStaticCredentialsProvider credentials =
            new AWSStaticCredentialsProvider(new BasicAWSCredentials("a", ""));
    private final String region = Regions.US_EAST_1.getName();

    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        String localstackURl = System.getProperty("localstack");
        if (null != localstackURl) {
            return (AmazonSQSAsync) createQueue(AmazonSQSAsyncClientBuilder
                    .standard()
                    .withEndpointConfiguration(getEndpointConfig(localstackURl))
                    .withCredentials(credentials)
                    .build());
        }
        return AmazonSQSAsyncClientBuilder
                .standard()
                .build();
    }

    @Primary
    @Bean
    public AmazonSQS amazonSQS() {
        String localstackURl = System.getProperty("localstack");
        if (null != localstackURl) {
            return createQueue(AmazonSQSClientBuilder
                    .standard()
                    .withEndpointConfiguration(getEndpointConfig(localstackURl))
                    .withCredentials(credentials)
                    .build());
        }
        return AmazonSQSClientBuilder
                .standard()
                .build();
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

    // Until localstack is built out more, create the queue here when running locally
    private AmazonSQS createQueue(AmazonSQS amazonSQS) {
        amazonSQS.createQueue("ab2d-job-tracking");
        return amazonSQS;
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfig(String localstackURl) {
        return new AwsClientBuilder.EndpointConfiguration(localstackURl, region);
    }

}
