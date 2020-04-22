package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KenesisConfig {
    @Value("${eventlogger.kinesis.region}")
    private String region;
    @Value("${eventlogger.kinesis.awsaccess.key}")
    private String key;
    @Value("${eventlogger.kinesis.awsaccess.secretkey}")
    private String secretKey;

    @Bean
    AmazonKinesisFirehose getProducer() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(key, secretKey);
        return AmazonKinesisFirehoseClient.builder()
                .withRegion(Regions.valueOf(region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
}
