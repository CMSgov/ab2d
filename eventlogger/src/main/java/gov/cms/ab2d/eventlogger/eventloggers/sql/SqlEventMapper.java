package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import org.springframework.jdbc.core.RowMapper;

public abstract class SqlEventMapper implements RowMapper {
    abstract void log(LoggableEvent event);
}
