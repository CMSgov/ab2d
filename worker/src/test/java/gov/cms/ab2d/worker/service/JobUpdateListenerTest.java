package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@Slf4j
@EnableSqs
class JobUpdateListenerTest {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JobUpdateListener listener;

    @Autowired
    private JobProgressUpdateService jobProgressUpdateService;

    @Autowired
    private JobProgressService jobProgressService;


    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    public static final LocalStackContainer localstack = new AB2DLocalstackContainer();

    @Test
    void jobUpdateQueue() throws JsonProcessingException {

        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        final String uuid = UUID.randomUUID().toString();
        log.info("sending queue uuid: {} old: {} new: {}", uuid, oldThreshold, newThreshold);
        final AmazonSQS sqs = AmazonSQSClient
                .builder()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(SQS))
                .withCredentials(localstack.getDefaultCredentialsProvider()).build();
        log.info("Job uuid {}", uuid);
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, JobMeasure.FAILURE_THRESHHOLD, oldThreshold);
        String mainQueueURL = sqs.getQueueUrl("ab2d-job-tracking").getQueueUrl();
        assertTrue(sqs.listQueues().getQueueUrls().contains(mainQueueURL));
        String update = mapper.writeValueAsString(createJobUpdate(uuid, newThreshold));
        log.info("Sending update {}", update);
        SendMessageResult messageResult = sqs.sendMessage(mainQueueURL, update);
        log.info("jobUpdateQueue test sending to ab2d-job-tracking {}", messageResult);
        log.info(sqs.receiveMessage(mainQueueURL).getMessages().toString());
        await().atMost(60, TimeUnit.SECONDS)
                .until(() ->
                        messageResult.getMessageId() != null
                                && jobProgressService.getStatus(uuid).getFailureThreshold() == newThreshold

                );
    }

    @Test
    void jobUpdateDirectly() {
        final int oldThreshold = new Random().nextInt();
        final int newThreshold = new Random().nextInt();
        String uuid = UUID.randomUUID().toString();
        log.info("sending directly uuid: {} old: {} new: {}", uuid, oldThreshold, newThreshold);
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, JobMeasure.FAILURE_THRESHHOLD, oldThreshold);
        listener.processJobProgressUpdate(createJobUpdate(uuid, newThreshold), Mockito.mock(Acknowledgment.class));
        assertEquals(newThreshold, jobProgressService.getStatus(uuid).getFailureThreshold());
    }

    private JobUpdate createJobUpdate(String uuid, int threshold) {
        return JobUpdate.builder()
                .jobUUID(uuid)
                .measure(JobMeasure.FAILURE_THRESHHOLD.toString())
                .value(threshold)
                .build();
    }

}
