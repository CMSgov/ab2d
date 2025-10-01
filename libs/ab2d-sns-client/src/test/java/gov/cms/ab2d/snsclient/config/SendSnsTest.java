package gov.cms.ab2d.snsclient.config;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.snsclient.clients.SNSClient;
import gov.cms.ab2d.snsclient.clients.SNSClientImpl;
import gov.cms.ab2d.snsclient.clients.SNSConfig;
import gov.cms.ab2d.snsclient.exception.SNSClientException;
import gov.cms.ab2d.snsclient.messages.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sns.SnsClient;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Testcontainers
class SendSnsTest {

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    private SNSClient snsClient;

    @Autowired
    private Ab2dEnvironment environment;

    @Test
    void testSendSns() {
        assertDoesNotThrow(() -> {
            snsClient.sendMessage(Topics.COVERAGE_COUNTS.getValue(), "test");
        });
    }

    @Test
    void testNoUrl() throws Exception {
        System.clearProperty("cloud.aws.end-point.uri");
        SNSConfig snsConfig = new SNSConfig();
        SnsClient amazonSns = snsConfig.amazonSNS();
        SNSClientImpl client = new SNSClientImpl(amazonSns, environment, "my-test-prefix");
        assertDoesNotThrow(() -> {
            client.sendMessage(Topics.COVERAGE_COUNTS.getValue(), "test");
        });
    }

    @Test
    void testNoPrefix() throws Exception {
        SNSConfig snsConfig = new SNSConfig();
        SnsClient amazonSns = snsConfig.amazonSNS();
        assertThrows(SNSClientException.class, () -> {
            new SNSClientImpl(amazonSns, environment);
        });
    }

    @Test
    void testEmptyPrefix() throws Exception {
        SNSConfig snsConfig = new SNSConfig();
        SnsClient amazonSns = snsConfig.amazonSNS();
        assertThrows(SNSClientException.class, () -> {
            new SNSClientImpl(amazonSns, environment, "");
        });
    }

}
