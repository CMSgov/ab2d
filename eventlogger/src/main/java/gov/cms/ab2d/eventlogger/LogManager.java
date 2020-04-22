package gov.cms.ab2d.eventlogger;

import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
public class LogManager {
    @Autowired
    private SqlEventLogger sqlEventLogger;
    @Autowired
    private KinesisEventLogger kinesisEventLogger;

    public enum LogType {
        SQL,
        KINESIS
    }

    public void log(LoggableEvent event) {
        EnumSet.allOf(LogType.class)
                .forEach(type -> log(type, event));
    }

    public void log(LogType type, LoggableEvent event) {
        switch (type) {
            case SQL:
                sqlEventLogger.log(event);
                break;
            case KINESIS:
                kinesisEventLogger.log(event);
                break;
            default:
                break;
        }
    }
}
