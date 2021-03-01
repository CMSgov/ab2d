package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;

class FileEventMapper extends SqlEventMapper {
    private final NamedParameterJdbcTemplate template;

    FileEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != FileEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + FileEvent.class.toString());
        }
        FileEvent be = (FileEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event_file " +
                " (time_of_event, organization, job_id, file_name, status, file_size, file_hash, aws_id, environment) " +
                " values (:time, :organization, :job, :fileName, :status, :fileSize, :fileHash, :awsId, :environment)";

        SqlParameterSource parameters = super.addSuperParams(event)
                .addValue("fileName", be.getFileName())
                .addValue("status", be.getStatus() != null ? be.getStatus().name() : null)
                .addValue("fileSize", be.getFileSize())
                .addValue("fileHash", be.getFileHash());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public FileEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        FileEvent event = new FileEvent();
        extractSuperParams(resultSet, event);

        event.setFileName(resultSet.getString("file_name"));
        event.setStatus(FileEvent.FileStatus.valueOf(resultSet.getString("status")));
        event.setFileSize(resultSet.getLong("file_size"));
        event.setFileHash(resultSet.getString("file_hash"));
        return event;
    }
}
