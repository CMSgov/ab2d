package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Defines what an SQL mapper should have - you should be able to log
 * and you should be able to read a the event from a result set
 */
public abstract class SqlEventMapper implements RowMapper {
    abstract void log(LoggableEvent event);

    static long getIdValue(KeyHolder keyHolder) {
        if (keyHolder == null || keyHolder.getKeys() == null) {
            return 0;
        }
        Integer val = (Integer) (keyHolder.getKeys().get("id"));
        if (val == null) {
            return 0;
        }
        return val.longValue();
    }
}
