package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

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

    MapSqlParameterSource addSuperParams(LoggableEvent event) {
        return new MapSqlParameterSource()
                .addValue("time", UtilMethods.convertToUtc(event.getTimeOfEvent()))
                .addValue("user", event.getUser())
                .addValue("job", event.getJobId())
                .addValue("awsId", event.getAwsId())
                .addValue("environment", event.getEnvironment());
    }

    void extractSuperParams(ResultSet rs, LoggableEvent event) throws SQLException {
        event.setId(rs.getLong("id"));
        event.setTimeOfEvent(rs.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(rs.getString("user_id"));
        event.setJobId(rs.getString("job_id"));
        event.setAwsId(rs.getString("aws_id"));
        event.setEnvironment(rs.getString("environment"));
    }
}
