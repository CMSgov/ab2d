package gov.cms.ab2d.eventlogger.utils;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


@Slf4j
public class AB2DLocalstackContainer extends LocalStackContainer {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:3.5.0");

    public AB2DLocalstackContainer() {
        super(IMAGE_VERSION);
        super.withServices(Service.SQS);
        super.withEnv("SQS_ENDPOINT_STRATEGY", "dynamic");
        waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));
    }

    @Override
    public void start() {
        super.start();

        System.setProperty("cloud.aws.stack.auto", "false");
        System.setProperty("cloud.aws.region.static", "us-east-1");
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "");
        System.setProperty("spring.cloud.aws.sqs.endpoint", getEndpointOverride(Service.SQS).toString());
        super.withServices(Service.SQS);

        System.setProperty("AWS_SQS_URL",
                getEndpointOverride(Service.SQS).toString());
//        System.setProperty("AWS_SQS_URL",
//                "http://localhost:" + this.getMappedPort(EnabledService.named("SQS").getPort()));
    }
}
