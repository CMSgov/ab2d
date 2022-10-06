package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ContractSearchEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.events.ReloadEvent;
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
        mapperMapping.put(ContractSearchEvent.class, new ContractSearchEventMapper(jdbcTemplate));
        mapperMapping.put(ErrorEvent.class, new ErrorEventMapper(jdbcTemplate));
        mapperMapping.put(FileEvent.class, new FileEventMapper(jdbcTemplate));
        mapperMapping.put(JobStatusChangeEvent.class, new JobStatusChangeEventMapper(jdbcTemplate));

        tableMapping.put(ApiRequestEvent.class, "event.event_api_request");
        tableMapping.put(ApiResponseEvent.class, "event.event_api_response");
        tableMapping.put(ReloadEvent.class, "event.event_bene_reload");
        tableMapping.put(ContractSearchEvent.class, "event.event_bene_search");
        tableMapping.put(ErrorEvent.class, "event.event_error");
        tableMapping.put(FileEvent.class, "event.event_file");
        tableMapping.put(JobStatusChangeEvent.class, "event.event_job_status_change");
    }

    public Set<Class<? extends LoggableEvent>> getClasses() {
        return tableMapping.keySet();
    }
}
