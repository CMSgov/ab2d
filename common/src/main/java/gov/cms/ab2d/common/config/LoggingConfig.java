package gov.cms.ab2d.common.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@AllArgsConstructor
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingConfig {

    private final Environment env;

    @PostConstruct
    public void init() {
        String jsonLogging = env.getProperty("json-logging");

        // Developers must add the json-logging env variable to their configuration and set it to false
        if (jsonLogging != null && jsonLogging.equals("false")) {
            // First remove the old loggers, including access logs
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender("jsonConsoleAppender");
            rootLogger.detachAppender("logstash-access");

            // Then add the new one that will be added to the console
            final ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
            ca.setContext(loggerContext);
            ca.setName("console");
            final LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
            encoder.setContext(loggerContext);
            final TTLLLayout layout = new TTLLLayout();
            layout.setContext(loggerContext);
            layout.start();
            encoder.setLayout(layout);
            ca.setEncoder(encoder);
            ca.start();
            rootLogger.setLevel(Level.INFO);
            rootLogger.addAppender(ca);
        }
    }
}
