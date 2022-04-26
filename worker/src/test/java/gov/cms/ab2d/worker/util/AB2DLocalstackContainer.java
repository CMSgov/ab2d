package gov.cms.ab2d.worker.util;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

public class AB2DLocalstackContainer extends LocalStackContainer {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:latest");

    public AB2DLocalstackContainer() {
        super(IMAGE_VERSION);
    }

    @Override
    public void start() {
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "");
        super.withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        super.withServices(SQS);
        super.start();
        System.setProperty("localstack",
                "localhost:" + this.getMappedPort(LocalStackContainer.EnabledService.named("SQS").getPort()));
        final AmazonSQS sqs = AmazonSQSClient
                .builder()
                .withEndpointConfiguration(super.getEndpointConfiguration(SQS))
                .withCredentials(super.getDefaultCredentialsProvider()).build();
        sqs.createQueue("ab2d-job-tracking");
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }
}
