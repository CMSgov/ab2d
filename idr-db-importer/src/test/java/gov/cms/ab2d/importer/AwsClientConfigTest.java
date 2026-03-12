package gov.cms.ab2d.importer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwsClientConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(AwsClientConfig.class);

    @Test
    void s3ClientDefaultRegionTest() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("s3Client"));
            S3Client client = context.getBean(S3Client.class);
            assertEquals(Region.US_EAST_1, client.serviceClientConfiguration().region());
        });
    }

    @Test
    void s3ClientProvidedRegion() {
        contextRunner
                .withPropertyValues("app.awsRegion=us-west-2")
                .run(context -> {
                    S3Client client = context.getBean(S3Client.class);
                    assertEquals(Region.US_WEST_2, client.serviceClientConfiguration().region());
                });
    }
}

