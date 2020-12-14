package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
@Slf4j
public class KinesisEventLogger implements EventLogger {
    @Value("${execution.env}")
    private String appEnv;
    @Value("${eventlogger.kinesis.stream.prefix:}")
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
        if (appEnv.equalsIgnoreCase("local")) {
            return;
        }
        try {
            LoggableEvent newLogEvent = event.clone();
            if (event.getUser() != null && !event.getUser().isEmpty()) {
                newLogEvent.setUser(DigestUtils.md5Hex(event.getUser()).toUpperCase());
            }
            ThreadPoolTaskExecutor ex = config.kinesisLogProcessingPool();
            KinesisEventProcessor processor = new KinesisEventProcessor(newLogEvent, client, streamId);
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
