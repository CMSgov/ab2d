package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class KinesisConfig {
    @Value("${eventlogger.kinesis.region:US_EAST_1}")
    private String region;

    @Value("${eventlogger.core.pool.size:5}")
    private int jobCorePoolSize;

    @Value("${eventlogger.max.pool.size:10}")
    private int jobMaxPoolSize;

    @Bean
    AmazonKinesisFirehose getProducer() {
        return AmazonKinesisFirehoseClient.builder()
                .withRegion(Regions.valueOf(region))
                .build();
    }

    @Bean
    public ThreadPoolTaskExecutor kinesisLogProcessingPool() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(jobCorePoolSize);
        taskExecutor.setMaxPoolSize(jobMaxPoolSize);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        taskExecutor.setThreadNamePrefix("kin-");
        return taskExecutor;
    }
}
