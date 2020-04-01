package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class FileEventMapper extends SqlEventMapper {
    private JdbcTemplate template;

    public FileEventMapper(JdbcTemplate template) {
        this.template = template;
    }
    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != FileEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + FileEvent.class.toString());
        }
        String query = "insert into event_file " +
                " (time_of_event, user_id, job_id, file_name, status, file_size, file_hash) " +
                " values (?, ?, ?, ?, ?, ?, ?)";

        FileEvent be = (FileEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] {"id"});
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setString(4, be.getFileName());
            ps.setString(5, be.getStatus() != null ? be.getStatus().name() : null);
            ps.setLong(6, be.getFileSize());
            ps.setString(7, be.getFileHash());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public FileEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        FileEvent event = new FileEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setFileName(resultSet.getString("file_name"));
        event.setStatus(FileEvent.FileStatus.valueOf(resultSet.getString("status")));
        event.setFileSize(resultSet.getLong("file_size"));
        event.setFileHash(resultSet.getString("file_hash"));
        return event;
    }
}
