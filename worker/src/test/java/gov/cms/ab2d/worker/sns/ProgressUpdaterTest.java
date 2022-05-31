package gov.cms.ab2d.worker.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.util.SnsMockUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.annotation.PostConstruct;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;

@Testcontainers
@Slf4j
@SpringBootTest
public class ProgressUpdaterTest {

    @TestConfiguration
    public static class EarlyConfiguration {
        @MockBean
        private AmazonSNS amazonSns;

        @PostConstruct
        public void initMock() {
            SnsMockUtil.mockSns(amazonSns);
        }
    }

    // Disable SNS
    @Autowired
    AmazonSNS amazonSns;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private ProgressUpdater progressUpdater;

    @Test
    void snsSubscribe() {
        assertDoesNotThrow(() -> {
            progressUpdater.confirmSubscriptionMessage(Mockito.mock(NotificationStatus.class));
        });
    }

    @Test
    void snsUnsubscribe() {
        assertDoesNotThrow(() -> {
            progressUpdater.confirmUnsubscribeMessage(Mockito.mock(NotificationStatus.class));
        });
    }


}
