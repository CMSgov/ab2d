package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class ApiResponseEventMapper extends SqlEventMapper {
    private JdbcTemplate template;

    public ApiResponseEventMapper(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ApiResponseEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ApiResponseEvent.class.toString());
        }
        String query = "insert into event_api_response " +
                " (time_of_event, user_id, job_id, response_code, response_string, description, request_id) " +
                " values (?, ?, ?, ?, ?, ?, ?)";

        ApiResponseEvent be = (ApiResponseEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] {"id"});
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setInt(4, be.getResponseCode());
            ps.setString(5, be.getResponseString());
            ps.setString(6, be.getDescription());
            ps.setString(7, be.getRequestId());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public ApiResponseEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ApiResponseEvent event = new ApiResponseEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setResponseCode(resultSet.getInt("response_code"));
        event.setResponseString(resultSet.getString("response_string"));
        event.setDescription(resultSet.getString("description"));
        event.setRequestId(resultSet.getString("request_id"));
        return event;
    }
}
