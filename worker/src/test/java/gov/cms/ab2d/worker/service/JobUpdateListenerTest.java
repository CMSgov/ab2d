package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import gov.cms.ab2d.worker.sns.ProgressUpdater;
import gov.cms.ab2d.worker.util.SnsMockUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.UUID;

import static gov.cms.ab2d.worker.processor.JobMeasure.FAILURE_THRESHHOLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;

@Testcontainers
@Slf4j
@SpringBootTest
class JobUpdateListenerTest {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JobProgressUpdateService jobProgressUpdateService;

    // We need some specific return values from the mocked AmazonSNS. They need to be set before ProgressUpdater
    // is created
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

    @Autowired
    private ProgressUpdater progressUpdater;

    @Autowired
    private JobProgressService jobProgressService;


    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    //Let's assume SNS works and call the SNS endpoint method directly
    @Test
    @DisplayName("Update job progress by calling SNS handler method")
    void jobUpdateDirectly() throws JsonProcessingException {
        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        String uuid = UUID.randomUUID().toString();
        log.info("sending directly uuid: {} old: {} new: {}", uuid, oldThreshold, newThreshold);
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, FAILURE_THRESHHOLD, oldThreshold);
        progressUpdater.receiveNotification(mapper.writeValueAsString(createJobUpdate(newThreshold)), uuid);
        assertEquals(newThreshold, jobProgressService.getStatus(uuid).getFailureThreshold());
    }

    @Test
    @DisplayName("Update job progress by calling SNS handler method")
    void jobUpdateDirectlyUnknown() throws JsonProcessingException {
        jobProgressUpdateService.addMeasure("invalid", FAILURE_THRESHHOLD, 1);
        progressUpdater.receiveNotification(mapper.writeValueAsString(createJobUpdate(2)), "invalid");
        assertNull(jobProgressService.getStatus("invalid"));
    }

    @Test
    @DisplayName("JobMeasure Test")
    void jobMeasure() throws JsonProcessingException {
        JobUpdate jobUpdate = createJobUpdate(10);
        assertEquals(10, jobUpdate.getValue());
        assertEquals(FAILURE_THRESHHOLD.toString(), jobUpdate.getMeasure());
    }

    private JobUpdate createJobUpdate(int threshold) throws JsonProcessingException {
        return JobUpdate.builder()
                .measure(FAILURE_THRESHHOLD.toString())
                .value(threshold)
                .build();
    }

}
