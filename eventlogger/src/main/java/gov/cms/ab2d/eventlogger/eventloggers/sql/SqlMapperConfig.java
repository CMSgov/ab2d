package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.BeneficiaryReloadEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps all the different event classes to their SQL serializers
 */
@Configuration
public class SqlMapperConfig {
    private Map<Class<? extends LoggableEvent>, SqlEventMapper> mapping = new HashMap<>() {
        {
            put(ApiRequestEvent.class, new ApiRequestEventMapper());
            put(ApiResponseEvent.class, new ApiResponseEventMapper());
            put(BeneficiaryReloadEvent.class, new BeneficiaryReloadEventMapper());
            put(ContractBeneSearchEvent.class, new ContractBeneSearchEventMapper());
            put(ErrorEvent.class, new ErrorEventSqlMapper());
            put(FileEvent.class, new FilesEventMapper());
        }
    };

    public SqlEventMapper getMapper(Class<? extends LoggableEvent> event) {
        return mapping.get(event);
    }
}
