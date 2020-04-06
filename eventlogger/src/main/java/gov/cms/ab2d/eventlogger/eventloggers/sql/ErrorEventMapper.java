package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class ErrorEventMapper extends SqlEventMapper {
    private NamedParameterJdbcTemplate template;

    ErrorEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ErrorEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ErrorEvent.class.toString());
        }
        ErrorEvent be = (ErrorEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event_error " +
                " (time_of_event, user_id, job_id, error_type, description) " +
                " values (:time, :user, :job, :errorType, :description)";

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("time", UtilMethods.convertToUtc(be.getTimeOfEvent()))
                .addValue("user", be.getUser())
                .addValue("job", be.getJobId())
                .addValue("errorType", be.getErrorType() != null ? be.getErrorType().name() : null)
                .addValue("description", be.getDescription());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public ErrorEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ErrorEvent event = new ErrorEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setErrorType(ErrorEvent.ErrorType.valueOf(resultSet.getString("error_type")));
        event.setDescription(resultSet.getString("description"));
        return event;
    }
}
