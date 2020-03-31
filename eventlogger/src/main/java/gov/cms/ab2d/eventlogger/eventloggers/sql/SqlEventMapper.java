package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;

public interface SqlEventMapper {
    void log(LoggableEvent event);
}
