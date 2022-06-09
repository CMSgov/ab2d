package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.dto.SNSMessage;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static gov.cms.ab2d.worker.processor.JobMeasure.FAILURE_THRESHHOLD;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@Slf4j
@SpringBootTest
public class JobUpdateListenerServiceITest {

    @Autowired
    private JobUpdateListenerServiceImpl jobUpdateListenerService;

    @Autowired
    private AmazonSQS amazonSqs;

    @Autowired
    private AmazonSNS amazonSns;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JobProgressUpdateService jobProgressUpdateService;

    @Autowired
    private JobProgressService jobProgressService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();


    @Test
    @DisplayName("SQS queue delete")
    void sqsCleanup() {
        JobUpdateListenerServiceImpl listener = new JobUpdateListenerServiceImpl(amazonSqs, amazonSns, mapper, jobProgressUpdateService);
        ReflectionTestUtils.invokeMethod(listener, "initiate");
        ReflectionTestUtils.invokeMethod(listener, "cleanup");
        assertThrows(QueueDoesNotExistException.class, () -> {
            String url = String.valueOf(ReflectionTestUtils.getField(jobUpdateListenerService, "queueUrl"));
            amazonSqs.getQueueUrl(url);
        });
    }

    @Test
    void pollSqs() throws JsonProcessingException {
        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        String url = String.valueOf(ReflectionTestUtils.getField(jobUpdateListenerService, "queueUrl"));
        jobProgressUpdateService.initJob("test");
        jobProgressUpdateService.addMeasure("test", FAILURE_THRESHHOLD, oldThreshold);
        SNSMessage snsMessage = new SNSMessage();
        snsMessage.setSubject("test");
        snsMessage.setMessage(mapper.writeValueAsString(JobUpdate.builder()
                .measure(String.valueOf(FAILURE_THRESHHOLD))
                .value(newThreshold)
                .build()));
        amazonSqs.sendMessage(url, mapper.writeValueAsString(snsMessage));
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                jobProgressService.getStatus("test").getFailureThreshold() != oldThreshold);
    }

    @Test
    void sqsError() {
        String url = String.valueOf(ReflectionTestUtils.getField(jobUpdateListenerService, "queueUrl"));
        assertDoesNotThrow(() ->
                amazonSqs.sendMessage(url, mapper.writeValueAsString(new SNSMessage()))
        );
    }

    @Test
    void unknownJob() throws JsonProcessingException {
        String url = String.valueOf(ReflectionTestUtils.getField(jobUpdateListenerService, "queueUrl"));
        SNSMessage snsMessage = new SNSMessage();
        snsMessage.setSubject("unknown");
        snsMessage.setMessage(mapper.writeValueAsString(JobUpdate.builder()
                .measure(String.valueOf(FAILURE_THRESHHOLD))
                .value(10)
                .build()));
        amazonSqs.sendMessage(url, mapper.writeValueAsString(snsMessage));
        assertDoesNotThrow(() ->
                amazonSqs.sendMessage(url, mapper.writeValueAsString(snsMessage))
        );
    }


    @Test
    @DisplayName("Update job progress by calling SNS handler method")
    void jobUpdateDirectly() {
        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        String uuid = UUID.randomUUID().toString();
        log.info("sending directly uuid: {} old: {} new: {}", uuid, oldThreshold, newThreshold);
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, FAILURE_THRESHHOLD, newThreshold);
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                newThreshold == jobProgressService.getStatus(uuid).getFailureThreshold()
        );
    }


}