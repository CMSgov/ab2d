package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ApiRequestEventMapper extends SqlEventMapper {
    private final NamedParameterJdbcTemplate template;

    ApiRequestEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ApiRequestEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ApiRequestEvent.class.toString());
        }
        ApiRequestEvent be = (ApiRequestEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event_api_request " +
                " (time_of_event, user_id, job_id, url, ip_address, token_hash, request_id, aws_id, environment) " +
                " values (:time, :user, :job, :url, :ipAddress, :tokenHash, :requestId, :awsId, :environment)";

        SqlParameterSource parameters = super.addSuperParams(event)
            .addValue("url", be.getUrl())
            .addValue("ipAddress", be.getIpAddress())
            .addValue("tokenHash", be.getTokenHash())
            .addValue("requestId", be.getRequestId());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public ApiRequestEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ApiRequestEvent event = new ApiRequestEvent();
        extractSuperParams(resultSet, event);

        event.setUrl(resultSet.getString("url"));
        event.setIpAddress(resultSet.getString("ip_address"));
        event.setTokenHash(resultSet.getString("token_hash"));
        event.setRequestId(resultSet.getString("request_id"));
        return event;
    }
}
