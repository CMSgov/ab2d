package gov.cms.ab2d.common.util;


import gov.cms.ab2d.eventclient.clients.SQSEventClient;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@TestConfiguration
@EnableAutoConfiguration
@Profile("mock-beans")
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
    return amazonSQSAsync;
  }

  @Bean
  @Primary
  public DefaultCredentialsProvider awsCredentialsProvider() {
    return DefaultCredentialsProvider.builder().build();
  }
}
