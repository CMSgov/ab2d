package gov.cms.ab2d.eventlogger;

import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import org.springframework.stereotype.Service;

@Service
public class LogManager {
    private final SqlEventLogger sqlEventLogger;
    private final KinesisEventLogger kinesisEventLogger;

    public LogManager(SqlEventLogger sqlEventLogger, KinesisEventLogger kinesisEventLogger) {
        this.sqlEventLogger = sqlEventLogger;
        this.kinesisEventLogger = kinesisEventLogger;
    }

    public enum LogType {
        SQL,
        KINESIS
    }

    public void log(LoggableEvent event) {
        // Save to the database
        sqlEventLogger.log(event);

        // This event will contain the db id, block so we can get the
        // aws id.
        kinesisEventLogger.log(event, true);

        // This event will not contain the AWS Id, update event in the DB
        sqlEventLogger.updateAwsId(event.getAwsId(), event);
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
