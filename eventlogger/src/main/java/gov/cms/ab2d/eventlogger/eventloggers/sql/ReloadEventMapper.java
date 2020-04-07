package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class ReloadEventMapper extends SqlEventMapper {
    private NamedParameterJdbcTemplate template;

    ReloadEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ReloadEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ReloadEvent.class.toString());
        }
        ReloadEvent be = (ReloadEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event_bene_reload " +
                " (time_of_event, user_id, job_id, file_type, file_name, number_loaded) " +
                " values (:time, :user, :job, :fileType, :fileName, :numLoaded)";

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("time", UtilMethods.convertToUtc(be.getTimeOfEvent()))
                .addValue("user", be.getUser())
                .addValue("job", be.getJobId())
                .addValue("fileType", be.getFileType() == null ? null : be.getFileType().name())
                .addValue("fileName", be.getFileName())
                .addValue("numLoaded", be.getNumberLoaded());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public ReloadEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ReloadEvent event = new ReloadEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setFileType(ReloadEvent.FileType.valueOf(resultSet.getString("file_type")));
        event.setFileName(resultSet.getString("file_name"));
        event.setNumberLoaded(resultSet.getInt("number_loaded"));

        return event;
    }
}
