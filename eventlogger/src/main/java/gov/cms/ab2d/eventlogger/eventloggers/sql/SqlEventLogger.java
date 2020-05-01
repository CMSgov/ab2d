package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SqlEventLogger implements EventLogger {
    @Value("${execution.env:dev}")
    private String appEnv;

    private final SqlMapperConfig mapperConfig;
    private final JdbcTemplate template;

    public SqlEventLogger(SqlMapperConfig mapperConfig, JdbcTemplate template) {
        this.mapperConfig = mapperConfig;
        this.template = template;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(LoggableEvent event) {
        event.setEnvironment(appEnv);
        try {
            SqlEventMapper mapper = mapperConfig.getMapper(event.getClass());
            if (mapper == null) {
                throw new EventLoggingException("Can't find SQL logger for " + event.getClass().toString());
            }
            mapper.log(event);
        } catch (Exception ex) {
            log.error("Error in logging event " + event.toString());
        }
    }

    public void updateAwsId(String awsId, LoggableEvent event) {
        if (event != null && awsId != null && event.getId() != null && event.getId() > 0) {
            this.template.update("UPDATE " + mapperConfig.getTableMapper(event.getClass()) +
                    " SET aws_id = ? WHERE id = ?", awsId, event.getId());
        }
    }
}
