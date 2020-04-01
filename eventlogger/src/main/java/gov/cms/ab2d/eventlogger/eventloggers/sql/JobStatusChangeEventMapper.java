package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class JobStatusChangeEventMapper extends SqlEventMapper {
    JdbcTemplate template;

    public JobStatusChangeEventMapper(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != JobStatusChangeEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + FileEvent.class.toString());
        }
        String query = "insert into event_job_status_change " +
                " (time_of_event, user_id, job_id, old_status, new_status, description) " +
                " values (?, ?, ?, ?, ?, ?)";

        JobStatusChangeEvent be = (JobStatusChangeEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] { "id" } );
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setString(4, be.getOldStatus() != null ? be.getOldStatus().name() : null);
            ps.setString(5, be.getNewStatus() != null ? be.getNewStatus().name() : null);
            ps.setString(6, be.getDescription());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public JobStatusChangeEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        JobStatusChangeEvent event = new JobStatusChangeEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));
        event.setOldStatus(JobStatus.valueOf(resultSet.getString("old_status")));
        event.setNewStatus(JobStatus.valueOf(resultSet.getString("new_status")));
        event.setDescription(resultSet.getString("description"));
        return event;
    }
}
