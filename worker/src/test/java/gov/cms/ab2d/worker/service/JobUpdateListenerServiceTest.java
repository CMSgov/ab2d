package gov.cms.ab2d.worker.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import gov.cms.ab2d.worker.util.AB2DLocalstackContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class JobUpdateListenerServiceTest {
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

        final AmazonSQS sqs = AmazonSQSClient
                .builder()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(SQS))
                .withCredentials(localstack.getDefaultCredentialsProvider()).build();
        String uuid = UUID.randomUUID().toString();
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, JobMeasure.FAILURE_THRESHHOLD, 10);

        String mainQueueURL = sqs.getQueueUrl("ab2d-job-tracking").getQueueUrl();
        assertTrue(sqs.listQueues().getQueueUrls().contains(mainQueueURL));
        SendMessageResult a = sqs.sendMessage(mainQueueURL, mapper.writeValueAsString(createJobUpdate(uuid)));
        System.out.println(a.getMD5OfMessageBody());
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> 100 == jobProgressService.getStatus(uuid).getFailureThreshold());
    }

    @Test
    void jobUpdateDirectly() {
        String uuid = UUID.randomUUID().toString();
        jobProgressUpdateService.initJob(uuid);
        jobProgressUpdateService.addMeasure(uuid, JobMeasure.FAILURE_THRESHHOLD, 10);
        listener.processJobProgressUpdate(createJobUpdate(uuid), Mockito.mock(Acknowledgment.class));
        assertEquals(100, jobProgressService.getStatus(uuid).getFailureThreshold());
    }

    private JobUpdate createJobUpdate(String uuid) {
        return JobUpdate.builder()
                .jobUUID(uuid)
                .measure(JobMeasure.FAILURE_THRESHHOLD.toString())
                .value(100)
                .build();
    }

}
