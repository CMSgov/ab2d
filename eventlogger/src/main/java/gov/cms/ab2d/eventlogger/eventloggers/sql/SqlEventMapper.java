package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import static gov.cms.ab2d.eventlogger.utils.UtilMethods.containsClientId;

/**
 * Defines what an SQL mapper should have - you should be able to log
 * and you should be able to read a the event from a result set
 */
@Slf4j
public abstract class SqlEventMapper implements RowMapper {
    abstract void log(LoggableEvent event);

    static long getIdValue(KeyHolder keyHolder) {
        //false NullPointerExceptions positives created in sonarqube
        if (keyHolder == null || keyHolder.getKeys() == null) {  //NOSONAR
            return 0;
        }
        Integer val = (Integer) (keyHolder.getKeys().get("id")); //NOSONAR
        if (val == null) {
            return 0;
        }
        return val.longValue();
    }

    MapSqlParameterSource addSuperParams(LoggableEvent event) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("time", event.getTimeOfEvent())
                .addValue("job", event.getJobId())
                .addValue("awsId", event.getAwsId())
                .addValue("environment", event.getEnvironment());

        if (containsClientId(event)) {
            log.error("Attempting to log event with timeOfEvent {} jobId {} which may contain an okta client id for its " +
                    "organization. Organization will be nulled out.", event.getTimeOfEvent(), event.getJobId());
            paramSource.addValue("organization", null);
        } else {
            paramSource.addValue("organization", event.getOrganization());
        }

        return paramSource;
    }

    void extractSuperParams(ResultSet rs, LoggableEvent event) throws SQLException {
        event.setId(rs.getLong("id"));
        event.setTimeOfEvent(rs.getObject("time_of_event", OffsetDateTime.class));
        event.setOrganization(rs.getString("organization"));
        event.setJobId(rs.getString("job_id"));
        event.setAwsId(rs.getString("aws_id"));
        event.setEnvironment(rs.getString("environment"));
    }
}
