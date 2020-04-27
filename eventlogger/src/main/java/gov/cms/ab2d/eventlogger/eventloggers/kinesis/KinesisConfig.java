package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KinesisConfig {
    @Value("${eventlogger.kinesis.region}")
    private String region;

    @Bean
    AmazonKinesisFirehose getProducer() {
        return AmazonKinesisFirehoseClient.builder()
                .withRegion(Regions.valueOf(region))
                .build();
    }
}
