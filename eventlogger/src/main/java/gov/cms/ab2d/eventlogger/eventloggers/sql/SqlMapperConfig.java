package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.BeneficiaryReloadEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps all the different event classes to their SQL serializers
 */
@Configuration
public class SqlMapperConfig {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<Class<? extends LoggableEvent>, SqlEventMapper> mapping = new HashMap<>();

    public SqlEventMapper getMapper(Class<? extends LoggableEvent> event) {
        if (mapping.isEmpty()) {
            init();
        }
        return mapping.get(event);
    }

    private void init() {
        mapping.put(ApiRequestEvent.class, new ApiRequestEventMapper(jdbcTemplate));
        mapping.put(ApiResponseEvent.class, new ApiResponseEventMapper(jdbcTemplate));
        mapping.put(BeneficiaryReloadEvent.class, new BeneficiaryReloadEventMapper(jdbcTemplate));
        mapping.put(ContractBeneSearchEvent.class, new ContractBeneSearchEventMapper(jdbcTemplate));
        mapping.put(ErrorEvent.class, new ErrorEventSqlMapper(jdbcTemplate));
        mapping.put(FileEvent.class, new FileEventMapper(jdbcTemplate));
        mapping.put(JobStatusChangeEvent.class, new JobStatusChangeEventMapper(jdbcTemplate));
    }
}
