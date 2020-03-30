package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SqlEventLogger implements EventLogger {
    @Autowired
    private SqlMapperConfig mapperConfig;

    @Override
    public void log(LoggableEvent event) {
        SqlEventMapper mapper = mapperConfig.getMapper(event.getClass());
        if (mapper == null) {
            throw new EventLoggingException("Can't find logger for " + event.getClass().toString());
        }
        mapper.log(event);
    }
}
