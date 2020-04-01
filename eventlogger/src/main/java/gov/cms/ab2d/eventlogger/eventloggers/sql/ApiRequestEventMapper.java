package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class ApiRequestEventMapper extends SqlEventMapper {
    private JdbcTemplate template;

    public ApiRequestEventMapper(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ApiRequestEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ApiRequestEvent.class.toString());
        }
        String query = "insert into event_api_request " +
                " (time_of_event, user_id, job_id, url, ip_address, token_hash, request_id) " +
                " values (?, ?, ?, ?, ?, ?, ?)";

        ApiRequestEvent be = (ApiRequestEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] {"id"});
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setString(4, be.getUrl());
            ps.setString(5, be.getIpAddress());
            ps.setString(6, be.getTokenHash());
            ps.setString(7, be.getRequestId());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public ApiRequestEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ApiRequestEvent event = new ApiRequestEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setUrl(resultSet.getString("url"));
        event.setIpAddress(resultSet.getString("ip_address"));
        event.setTokenHash(resultSet.getString("token_hash"));
        event.setRequestId(resultSet.getString("request_id"));
        return event;
    }
}
