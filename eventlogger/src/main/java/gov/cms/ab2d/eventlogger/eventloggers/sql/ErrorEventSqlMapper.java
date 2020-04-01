package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;

public class ErrorEventSqlMapper implements SqlEventMapper {
    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ErrorEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ErrorEvent.class.toString());
        }
    }
}
