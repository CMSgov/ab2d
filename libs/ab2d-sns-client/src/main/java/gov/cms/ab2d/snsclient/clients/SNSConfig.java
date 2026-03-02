package gov.cms.ab2d.snsclient.clients;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Slf4j
public class SNSConfig {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.end-point.uri}")
    private String url;

    @Value("${ab2d.sns.topic-prefix}")
    private String snsTopicPrefix;

    @Bean
    public SnsClient amazonSNS() throws URISyntaxException {
        log.info("Localstack url " + url);
        // only use the injected url locally, let aws figure itself out when deployed
        if ((url + "").contains("localhost")) {
            return SnsClient.builder()
                    .endpointOverride(new URI(url))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")))
                    .region(Region.US_EAST_1)
                    .build();
        }
        return SnsClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean
    public SNSClient snsClient(SnsClient amazonSNS, Ab2dEnvironment environment) {
        return new SNSClientImpl(amazonSNS, environment, snsTopicPrefix);
    }

}
