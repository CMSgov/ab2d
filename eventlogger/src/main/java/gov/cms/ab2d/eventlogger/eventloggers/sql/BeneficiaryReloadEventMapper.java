package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.BeneficiaryReloadEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class BeneficiaryReloadEventMapper extends SqlEventMapper {
    private JdbcTemplate template;

    public BeneficiaryReloadEventMapper(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != BeneficiaryReloadEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + BeneficiaryReloadEvent.class.toString());
        }
        String query = "insert into event_bene_reload " +
                " (time_of_event, user_id, job_id, file_type, file_name, number_loaded) " +
                " values (?, ?, ?, ?, ?, ?)";
        BeneficiaryReloadEvent be = (BeneficiaryReloadEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] {"id"});
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setString(4, be.getFileType() == null ? null : be.getFileType().name());
            ps.setString(5, be.getFileName());
            ps.setInt(6, be.getNumberLoaded());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public BeneficiaryReloadEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        BeneficiaryReloadEvent event = new BeneficiaryReloadEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setFileType(BeneficiaryReloadEvent.FileType.valueOf(resultSet.getString("file_type")));
        event.setFileName(resultSet.getString("file_name"));
        event.setNumberLoaded(resultSet.getInt("number_loaded"));

        return event;
    }
}
