package gov.cms.ab2d.common.util;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.InetAddress;


@Slf4j
public class AB2DLocalstackContainer extends LocalStackContainer {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:latest");

    public AB2DLocalstackContainer() {
        super(IMAGE_VERSION);
    }

    @SneakyThrows
    @Override
    public void start() {
        System.setProperty("cloud.aws.stack.auto","false");
        System.setProperty("cloud.aws.region.static","us-east-1");
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "");

        System.setProperty("ab2d.sns.base.address", "http://host.docker.internal:8080");
        super.withServices(Service.SQS, Service.SNS);
        //`super.withExtraHost("host.docker.internal","host-gateway");
        super.withExtraHost("host.docker.internal","172.17.0.1");
        super.start();
        // This is used in the SNS/SQS beans so bypass the normal AWS settings
        System.setProperty("LOCALSTACK_URL",
                "localhost:" + this.getMappedPort(LocalStackContainer.EnabledService.named("SNS").getPort()));
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }

}
