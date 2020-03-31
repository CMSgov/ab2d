package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;

public class ApiResponseEventMapper implements SqlEventMapper {
    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ApiResponseEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ApiResponseEvent.class.toString());
        }
    }
}
