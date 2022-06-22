package gov.cms.ab2d.worker.config;

import com.amazonaws.client.builder.AwsClientBuilder;
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
import java.util.Objects;
import java.util.stream.Stream;

import static gov.cms.ab2d.worker.config.LocalstackConfig.configureBuilder;

@Configuration
@Slf4j
public class SQSConfig {

    static {
        System.setProperty("AWS_CONTAINER_CREDENTIALS_FULL_URI",
                Stream.of("ECS_CONTAINER_METADATA_URI_V4", "ECS_CONTAINER_METADATA_URI")
                        .map(System::getProperty)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("http://169.254.169.254/get-credentials"));
    }
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
        return (AmazonSQS) configureBuilder(builder);
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

}
