package gov.cms.ab2d.common.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import io.awspring.cloud.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.messaging.listener.QueueMessageHandler;

import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableAutoConfiguration
public class AB2DSQSMockConfig {

  static {
    System.setProperty("feature.sqs.enabled", "false");
  }

  @MockBean
  AmazonSQSAsync amazonSQSAsync;

  @MockBean
  SQSEventClient sQSEventClient;

  @Bean
  @Primary
  public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
    SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
    factory.setAutoStartup(false);
    return factory;
  }

  @Bean
  public QueueMessageHandler messageHandler() {
    return mock(QueueMessageHandler.class);
  }

  // @Bean("mockAmazonSQS")
  // @Primary
  public AmazonSQSAsync amazonSQSAsync() {
    // return mock(AmazonSQSAsync.class);
    return amazonSQSAsync;
  }

  @Bean
  @Primary
  public AWSCredentialsProvider awsCredentialsProvider() {
    return new DefaultAWSCredentialsProviderChain();
  }
}