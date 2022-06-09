package gov.cms.ab2d.common.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;


@Slf4j
public class AB2DLocalstackContainer extends LocalStackContainer {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:0.14.3");

    public AB2DLocalstackContainer() {
        super(IMAGE_VERSION);
    }

    @SneakyThrows
    @Override
    public void start() {
        // These are used in the SNS/SQS beans to bypass the default AWS settings
        System.setProperty("cloud.aws.stack.auto", "false");
        System.setProperty("cloud.aws.region.static", "us-east-1");
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "");

        super.withServices(Service.SQS, Service.SNS);
        super.addEnv("LS_LOG", "trace");
        // this is buggy. Re-enable when it's fixed
        super.addEnv("SKIP_SSL_CERT_DOWNLOAD", "1");
        super.start();
        System.setProperty("LOCALSTACK_URL",
                "localhost:" + this.getMappedPort(LocalStackContainer.EnabledService.named("SNS").getPort()));
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }

}
