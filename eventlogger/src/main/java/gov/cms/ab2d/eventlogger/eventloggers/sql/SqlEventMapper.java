package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import org.springframework.jdbc.core.RowMapper;

/**
 * Defines what an SQL mapper should have - you should be able to log
 * and you should be able to read a the event from a result set
 */
public abstract class SqlEventMapper implements RowMapper {
    abstract void log(LoggableEvent event);
}
