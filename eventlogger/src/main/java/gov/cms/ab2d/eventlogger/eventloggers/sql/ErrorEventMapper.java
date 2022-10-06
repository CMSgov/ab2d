package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ErrorEventMapper extends SqlEventMapper {
    private final NamedParameterJdbcTemplate template;

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
        String query = "insert into event.event_error " +
                " (time_of_event, organization, job_id, error_type, description, aws_id, environment) " +
                " values (:time, :organization, :job, :errorType, :description, :awsId, :environment)";

        SqlParameterSource parameters = super.addSuperParams(event)
                .addValue("errorType", be.getErrorType() != null ? be.getErrorType().name() : null)
                .addValue("description", be.getDescription());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public ErrorEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ErrorEvent event = new ErrorEvent();
        extractSuperParams(resultSet, event);

        event.setErrorType(ErrorEvent.ErrorType.valueOf(resultSet.getString("error_type")));
        event.setDescription(resultSet.getString("description"));

        return event;
    }
}
