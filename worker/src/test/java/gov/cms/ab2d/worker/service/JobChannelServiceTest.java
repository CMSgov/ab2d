package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.util.SnsMockUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.annotation.PostConstruct;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
@Slf4j
@SpringBootTest
class JobChannelServiceTest {

    @TestConfiguration
    public static class EarlyConfiguration {
        @MockBean
        private AmazonSNS amazonSns;

        @PostConstruct
        public void initMock() {
            SnsMockUtil.mockSns(amazonSns);
        }
    }

    @Autowired
    private JobChannelServiceImpl jobChannelService;

    @Autowired
    private AmazonSNS amazonSns;

    @Autowired
    private PropertiesService propertiesService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @ParameterizedTest
    @ValueSource(strings = {"idle", "engaged"})
    void sendUpdate(String state) {
        PropertiesDTO snsState = new PropertiesDTO();
        snsState.setKey(Constants.SNS_JOB_UPDATE_ENGAGEMENT);
        snsState.setValue(state);
        SnsMockUtil.mockSns(amazonSns);
        propertiesService.updateProperties(List.of(snsState));
        assertDoesNotThrow(() -> {
            jobChannelService.sendUpdate("test", JobMeasure.FAILURE_THRESHHOLD, 1);
        });
    }


}
