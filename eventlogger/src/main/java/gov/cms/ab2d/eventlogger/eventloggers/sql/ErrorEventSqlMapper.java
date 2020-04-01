package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class ErrorEventSqlMapper extends SqlEventMapper {
    private JdbcTemplate template;

    public ErrorEventSqlMapper(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ErrorEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ErrorEvent.class.toString());
        }

        String query = "insert into event_error " +
                " (time_of_event, user_id, job_id, error_type, description) " +
                " values (?, ?, ?, ?, ?)";

        ErrorEvent be = (ErrorEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] {"id"});
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setString(4, be.getErrorType() != null ? be.getErrorType().name() : null);
            ps.setString(5, be.getDescription());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
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
