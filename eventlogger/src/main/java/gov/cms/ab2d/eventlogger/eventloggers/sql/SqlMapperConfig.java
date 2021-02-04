package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps all the different event classes to their SQL serializers
 */
@RequiredArgsConstructor
@Configuration
public class SqlMapperConfig {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final Map<Class<? extends LoggableEvent>, SqlEventMapper> mapperMapping = new HashMap<>();
    private final Map<Class<? extends LoggableEvent>, String> tableMapping = new HashMap<>();

    public SqlEventMapper getMapper(Class<? extends LoggableEvent> event) {
        if (mapperMapping.isEmpty()) {
            init();
        }
        return mapperMapping.get(event);
    }

    public String getTableMapper(Class<? extends LoggableEvent> event) {
        if (tableMapping.isEmpty()) {
            init();
        }
        return tableMapping.get(event);
    }

    private void init() {
        mapperMapping.put(ApiRequestEvent.class, new ApiRequestEventMapper(jdbcTemplate));
        mapperMapping.put(ApiResponseEvent.class, new ApiResponseEventMapper(jdbcTemplate));
        mapperMapping.put(ReloadEvent.class, new ReloadEventMapper(jdbcTemplate));
        mapperMapping.put(ContractBeneSearchEvent.class, new ContractBeneSearchEventMapper(jdbcTemplate));
        mapperMapping.put(ErrorEvent.class, new ErrorEventMapper(jdbcTemplate));
        mapperMapping.put(FileEvent.class, new FileEventMapper(jdbcTemplate));
        mapperMapping.put(JobStatusChangeEvent.class, new JobStatusChangeEventMapper(jdbcTemplate));

        tableMapping.put(ApiRequestEvent.class, "event_api_request");
        tableMapping.put(ApiResponseEvent.class, "event_api_response");
        tableMapping.put(ReloadEvent.class, "event_bene_reload");
        tableMapping.put(ContractBeneSearchEvent.class, "event_bene_search");
        tableMapping.put(ErrorEvent.class, "event_error");
        tableMapping.put(FileEvent.class, "event_file");
        tableMapping.put(JobStatusChangeEvent.class, "event_job_status_change");
    }

    public Set<Class<? extends LoggableEvent>> getClasses() {
        return tableMapping.keySet();
    }
}
