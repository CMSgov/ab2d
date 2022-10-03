package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PropertySource("classpath:application.eventlogger.properties")
@Slf4j
public class SqlEventLogger implements EventLogger {

    private final SqlMapperConfig mapperConfig;
    private final JdbcTemplate template;
    private final Ab2dEnvironment ab2dEnvironment;

    public SqlEventLogger(SqlMapperConfig mapperConfig, JdbcTemplate template,
                          Ab2dEnvironment ab2dEnvironment) {
        this.mapperConfig = mapperConfig;
        this.template = template;
        this.ab2dEnvironment = ab2dEnvironment;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(LoggableEvent event) {
        event.setEnvironment(ab2dEnvironment);

        try {
            SqlEventMapper mapper = mapperConfig.getMapper(event.getClass());
            if (mapper == null) {
                throw new EventLoggingException("Can't find SQL logger for " + event.getClass().toString());
            }
            mapper.log(event);
        } catch (Exception ex) {
            log.error("Error in logging event " + event);
        }
    }

    public void updateAwsId(String awsId, LoggableEvent event) {
        if (event != null && awsId != null && event.getId() != null && event.getId() > 0) {
            this.template.update("UPDATE " + mapperConfig.getTableMapper(event.getClass()) +
                    " SET aws_id = ? WHERE id = ?", awsId, event.getId());
        }
    }
}
