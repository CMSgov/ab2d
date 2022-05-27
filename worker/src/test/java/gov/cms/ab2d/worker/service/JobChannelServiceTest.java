package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.processor.JobMeasure;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.anyString;

@Testcontainers
@Slf4j
@SpringBootTest
public class JobChannelServiceTest {

    @TestConfiguration
    public static class EarlyConfiguration {
        @MockBean
        private AmazonSNS amazonSns;

        @PostConstruct
        public void initMock() {
            CreateTopicResult result = new CreateTopicResult();
            result.setTopicArn("");
            Mockito.when(amazonSns.createTopic(anyString())).thenReturn(result);
            Mockito.when(amazonSns.subscribe(anyString(), anyString(), anyString())).thenReturn(new SubscribeResult());
        }
    }
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JobChannelService jobChannelService;

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
        propertiesService.updateProperties(List.of(snsState));
        assertDoesNotThrow(() -> {
            jobChannelService.sendUpdate("test", JobMeasure.FAILURE_THRESHHOLD, 1);
        });
    }


}
