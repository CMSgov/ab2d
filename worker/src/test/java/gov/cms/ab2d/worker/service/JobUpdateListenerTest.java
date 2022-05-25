package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.worker.sns.ProgressUpdater;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@Slf4j
class JobUpdateListenerTest {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ProgressUpdater progressUpdater;

    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private JobProgressUpdateService jobProgressUpdateService;

    @Autowired
    private JobProgressService jobProgressService;

    //disable sns
    @MockBean
    private AmazonSNS amazonSNS;


    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    public static final LocalStackContainer localstack = new AB2DLocalstackContainer();

    @Test
    @DisplayName("Update job progress over SNS")
    @Disabled("Disable until localstack sns works")
    void jobUpdateQueue() throws JsonProcessingException {
        PropertiesDTO snsEngage = new PropertiesDTO();
        snsEngage.setKey(Constants.SNS_JOB_UPDATE_ENGAGEMENT);
        snsEngage.setValue("engaged");

        propertiesService.updateProperties(List.of(snsEngage));
        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        final String uuid = UUID.randomUUID().toString();
        log.info("sending queue uuid: {} old: {} new: {}", uuid, oldThreshold, newThreshold);
        final AmazonSNS sns = AmazonSNSClient
                .builder()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(SNS))
                .withCredentials(localstack.getDefaultCredentialsProvider()).build();
        log.info("Job uuid {}", uuid);
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, JobMeasure.FAILURE_THRESHHOLD, oldThreshold);
        String topicArn = sns.createTopic("ab2d-job-tracking").getTopicArn();
        assertTrue(sns.listTopics()
                .getTopics()
                .stream()
                .map(Topic::getTopicArn)
                .anyMatch(topic -> topic.equals(topicArn)));
        String update = mapper.writeValueAsString(createJobUpdate(newThreshold));
        log.info("Sending update {}", update);
        PublishResult messageResult = sns.publish(topicArn, createJobUpdate(newThreshold), uuid);
        log.info("jobUpdateQueue test sending to ab2d-job-tracking {}", messageResult);
        await().atMost(60, TimeUnit.SECONDS)
                .until(() ->
                        messageResult.getMessageId() != null
                                && jobProgressService.getStatus(uuid).getFailureThreshold() == newThreshold

                );
    }

    @Test
    @DisplayName("Update job progress by calling SNS handler method")
    void jobUpdateDirectly() throws JsonProcessingException {
        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        String uuid = UUID.randomUUID().toString();
        log.info("sending directly uuid: {} old: {} new: {}", uuid, oldThreshold, newThreshold);
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, JobMeasure.FAILURE_THRESHHOLD, oldThreshold);
        progressUpdater.receiveNotification(createJobUpdate(newThreshold), uuid);
        assertEquals(newThreshold, jobProgressService.getStatus(uuid).getFailureThreshold());
    }

    private String createJobUpdate(int threshold) throws JsonProcessingException {
        return mapper.writeValueAsString(
                JobUpdate.builder()
                        .measure(JobMeasure.FAILURE_THRESHHOLD.toString())
                        .value(threshold)
                        .build());
    }

}
