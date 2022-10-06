package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.EventLoggingException;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JobStatusChangeEventMapper extends SqlEventMapper {
    private final NamedParameterJdbcTemplate template;

    JobStatusChangeEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != JobStatusChangeEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + FileEvent.class.toString());
        }
        JobStatusChangeEvent be = (JobStatusChangeEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event.event_job_status_change " +
                " (time_of_event, organization, job_id, old_status, new_status, description, aws_id, environment) " +
                " values (:time, :organization, :job, :oldStatus, :newStatus, :description, :awsId, :environment)";

        SqlParameterSource parameters = super.addSuperParams(event)
                .addValue("oldStatus", be.getOldStatus())
                .addValue("newStatus", be.getNewStatus())
                .addValue("description", be.getDescription());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public JobStatusChangeEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        JobStatusChangeEvent event = new JobStatusChangeEvent();
        extractSuperParams(resultSet, event);

        event.setOldStatus(resultSet.getString("old_status"));
        event.setNewStatus(resultSet.getString("new_status"));
        event.setDescription(resultSet.getString("description"));
        return event;
    }
}
