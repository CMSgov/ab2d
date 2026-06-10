package gov.cms.ab2d.contracts.util;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableAutoConfiguration
public class AB2DSQSMockConfig {

  static {
    System.setProperty("feature.sqs.enabled", "false");
  }

  @MockitoBean
  SqsAsyncClient amazonSQSAsync;

  @MockitoBean
  SQSEventClient sQSEventClient;

  @Bean("mockAmazonSQS")
  public SqsAsyncClient amazonSQSAsync() {
    return mock(SqsAsyncClient.class);
  }
}
