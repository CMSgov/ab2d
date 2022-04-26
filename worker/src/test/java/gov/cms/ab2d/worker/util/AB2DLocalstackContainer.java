package gov.cms.ab2d.worker.util;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Slf4j
public class AB2DLocalstackContainer extends LocalStackContainer {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:latest");

    public AB2DLocalstackContainer() {
        super(IMAGE_VERSION);
    }

    @Override
    public void start() {
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "");
        super.withServices(SQS);
        super.start();
        System.setProperty("localstack",
                "localhost:" + this.getMappedPort(LocalStackContainer.EnabledService.named("SQS").getPort()));
        createQueue();
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }

    private void createQueue() {
        final AmazonSQS sqs = AmazonSQSClient
                .builder()
                .withEndpointConfiguration(super.getEndpointConfiguration(SQS))
                .withCredentials(super.getDefaultCredentialsProvider()).build();
        String jobTrackingQueue = "ab2d-job-tracking";
        try {
            sqs.getQueueUrl(jobTrackingQueue);
            log.info(jobTrackingQueue + " already exists");
        } catch (Exception ignored) {
            sqs.createQueue(jobTrackingQueue);
            log.info(jobTrackingQueue + " created");

        }
    }

}
