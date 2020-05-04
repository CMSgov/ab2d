package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
@Slf4j
public class KinesisEventLogger implements EventLogger {
    @Value("${execution.env:dev}")
    private String appEnv;
    @Value("${eventlogger.kinesis.stream.prefix:bfd-insights-ab2d-}")
    private String streamId;

    private final KinesisConfig config;
    private final AmazonKinesisFirehose client;

    public KinesisEventLogger(KinesisConfig config, AmazonKinesisFirehose client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public void log(LoggableEvent event) {
        log(event, false);
    }

    public void log(LoggableEvent event, boolean block) {
        event.setEnvironment(appEnv);
        ThreadPoolTaskExecutor ex = config.kinesisLogProcessingPool();
        KinesisEventProcessor processor = new KinesisEventProcessor(event, client, streamId);
        Future<Void> future = ex.submit(processor);
        if (block) {
            while (!future.isDone()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    log.error("Unable to wait for thread to sleep", e);
                }
            }
        }
    }

    boolean isFinished() {
        ThreadPoolTaskExecutor ex = config.kinesisLogProcessingPool();
        return ex.getActiveCount() == 0;
    }
}
