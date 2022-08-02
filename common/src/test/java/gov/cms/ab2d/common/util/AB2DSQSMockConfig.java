package gov.cms.ab2d.common.util;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration;
import org.springframework.cloud.aws.autoconfigure.messaging.MessagingAutoConfiguration;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableAutoConfiguration(exclude = {MessagingAutoConfiguration.class, ContextStackAutoConfiguration.class})
public class AB2DSQSMockConfig {
  @MockBean
  AmazonSQSAsync amazonSQSAsync;

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

  @Bean
  public AmazonSQSAsync amazonSQSAsync() {
    return mock(AmazonSQSAsync.class);
  }
}