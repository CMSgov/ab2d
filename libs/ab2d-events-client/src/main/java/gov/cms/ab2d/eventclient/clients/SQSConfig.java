package gov.cms.ab2d.eventclient.clients;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.eventclient.EventClientException;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.net.URI;

@Import(SqsBootstrapConfiguration.class)
@Configuration
@Slf4j
public class SQSConfig {

    private final String sqsQueueName;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.end-point.uri}")
    private String url;

    public SQSConfig(@Value("${cloud.aws.region.static}") String region,
                     @Value("${cloud.aws.end-point.uri}") String url,
                     Ab2dEnvironment ab2dEnvironment) {
        this.region = region;
        this.url = url;

        this.sqsQueueName = (ab2dEnvironment == Ab2dEnvironment.LOCAL)
                ? "local-events-sqs"
                : deriveSqsQueueName(url);

        // This is needed so the sqsListener can get the queue name.
        // It only accepts constance and this is a way to get around that while dynamically setting a sqs name
        System.setProperty("sqs.queue-name", sqsQueueName);
        log.info("Setting SQS queue name: '{}'", sqsQueueName);
    }

    public static String deriveSqsQueueName(String url) {
        try {
            String[] tokens = url.split("/");
            return tokens[tokens.length-1];
        }
        catch (Exception e) {
            throw new EventClientException("Unable to derive SQS queue name from URL: " + url);
        }
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory
                .builder()
                .configure(options -> options
                        .messageConverter(sqsMessagingMessageConverter()))
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }

    @Primary
    @Bean
    public SqsAsyncClient amazonSQSAsync() {
        log.info("Locakstack url " + url);
        if (url != null) {
            return createQueue(SqsAsyncClient.builder()
                    .credentialsProvider(DefaultCredentialsProvider.builder().build())
                    .endpointOverride(URI.create(url))
                    .region(Region.US_EAST_1)
                    .build());
        }
        log.info("Creating SqsAsyncClient");
        return SqsAsyncClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean
    public SQSEventClient sqsEventClient(SqsAsyncClient amazonSQS) {
        return new SQSEventClient(amazonSQS, objectMapper(), sqsQueueName);
    }

    /*
        This is a static method to avoid breaking existing projects mapping.
     */
    public static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }

    @Bean
    protected MessageConverter jacksonMessageConverter() {
        MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
        jacksonMessageConverter.setObjectMapper(objectMapper());
        jacksonMessageConverter.setSerializedPayloadClass(String.class);
        jacksonMessageConverter.setStrictContentTypeMatch(false);
        return jacksonMessageConverter;
    }

    private SqsMessagingMessageConverter sqsMessagingMessageConverter() {
        SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
        converter.setPayloadMessageConverter(jacksonMessageConverter());
        return converter;
    }

    public SqsAsyncClient createQueue(SqsAsyncClient sqsClient) {
        try {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(sqsQueueName)
                    .build();

            sqsClient.createQueue(createQueueRequest);
            log.info("Queue created: " + sqsQueueName);
        } catch (SqsException e) {
            log.error(e.getMessage());
        }
        return sqsClient;
    }

}
