package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@PropertySource("classpath:application.eventlogger.properties")
@SuppressWarnings("java:S2142")
@Slf4j
public class KinesisEventLogger implements EventLogger {

    private final KinesisConfig config;
    private final AmazonKinesisFirehose client;
    private final Ab2dEnvironment ab2dEnvironment;
    private final KinesisMode kinesisEnabled;
    private final String streamId;

    public KinesisEventLogger(KinesisConfig config, AmazonKinesisFirehose client,
                              Ab2dEnvironment appEnv,
                              @Value("${eventlogger.kinesis.enabled}") KinesisMode kinesisEnabled,
                              @Value("${eventlogger.kinesis.stream.prefix:}") String streamId) {
        this.config = config;
        this.client = client;
        this.ab2dEnvironment = appEnv;
        this.kinesisEnabled = kinesisEnabled;
        this.streamId = streamId;
    }

    @Override
    public void log(LoggableEvent event) {
        log(event, false);
    }

    public void log(LoggableEvent event, boolean block) {
        event.setEnvironment(ab2dEnvironment);

        // If kinesis is disabled then return immediately
        if (kinesisEnabled == KinesisMode.NONE) {
            return;
        }

        // Otherwise assume logging is functional
        try {
            ThreadPoolTaskExecutor ex = config.kinesisLogProcessingPool();
            KinesisEventProcessor processor = new KinesisEventProcessor(event, client, streamId);
            Future<Void> future = ex.submit(processor);
            if (block) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("Unable to wait for thread to sleep", e);
                }
            }
        } catch (Exception ex) {
            // Logging should never break anything
            log.error("Unable to do Kinesis logging", ex);
        }
    }

    boolean isFinished() {
        ThreadPoolTaskExecutor ex = config.kinesisLogProcessingPool();
        return ex.getActiveCount() == 0;
    }
}
