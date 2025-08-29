package gov.cms.ab2d.snsclient.clients;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Slf4j
public class SNSConfig {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.end-point.uri}")
    private String url;


    @Bean
    public SnsClient amazonSNS() throws URISyntaxException {
        log.info("Locakstack url " + url);
        // only use the injected url locally, let aws figure itself out when deployed
        return ((url + "").contains("localhost")
                        ? SnsClient.builder()
                        .endpointOverride(new URI(url))
                        .region(Region.US_EAST_1)
                        : SnsClient.builder()
                        .region(Region.US_EAST_1)
                ).build();
    }

    @Bean
    public SNSClient snsClient(SnsClient amazonSNS, Ab2dEnvironment environment) {
        return new SNSClientImpl(amazonSNS, environment);
    }

}
