package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SqlEventLogger implements EventLogger {
    private final SqlMapperConfig mapperConfig;

    public SqlEventLogger(SqlMapperConfig mapperConfig) {
        this.mapperConfig = mapperConfig;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(LoggableEvent event) {
        try {
            SqlEventMapper mapper = mapperConfig.getMapper(event.getClass());
            if (mapper == null) {
                throw new EventLoggingException("Can't find logger for " + event.getClass().toString());
            }
            mapper.log(event);
        } catch (Exception ex) {
            log.error("Error in logging event " + event.toString());
        }
    }
}
