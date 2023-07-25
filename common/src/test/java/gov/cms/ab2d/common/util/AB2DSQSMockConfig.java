package gov.cms.ab2d.common.util;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import io.awspring.cloud.autoconfigure.context.ContextStackAutoConfiguration;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import io.awspring.cloud.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.messaging.listener.QueueMessageHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
    SqsAutoConfiguration.class,
    ContextStackAutoConfiguration.class,
    ContextResourceLoaderAutoConfiguration.class,
})
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

  @Bean("mockAmazonSQS")
  public AmazonSQSAsync amazonSQSAsync() {
    return mock(AmazonSQSAsync.class);
  }
}
