package gov.cms.ab2d.coverage.util;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;


@Slf4j
public class AB2DCoverageLocalstackContainer extends LocalStackContainer {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:1.17.2");

    public AB2DCoverageLocalstackContainer() {
        super(IMAGE_VERSION);
    }

    @Override
    public void start() {
        System.setProperty("cloud.aws.stack.auto", "false");
        System.setProperty("cloud.aws.region.static", "us-east-1");
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "");
        super.withServices(Service.SQS);
        super.start();
        System.setProperty("AWS_SQS_URL",
                "localhost:" + this.getMappedPort(EnabledService.named("SQS").getPort()));
    }
}
