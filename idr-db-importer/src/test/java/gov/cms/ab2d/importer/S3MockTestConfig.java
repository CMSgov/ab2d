package gov.cms.ab2d.importer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
public class S3MockTestConfig {

    @Bean
    public S3Client s3Client() {
        // Use the client created by S3MockAPIExtension
        return S3MockAPIExtension.S3_CLIENT;
    }
}
