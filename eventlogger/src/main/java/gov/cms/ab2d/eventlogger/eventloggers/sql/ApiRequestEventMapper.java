package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;

public class ApiRequestEventMapper implements SqlEventMapper {
    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ApiRequestEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ApiRequestEvent.class.toString());
        }
    }
}
