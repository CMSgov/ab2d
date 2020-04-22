package gov.cms.ab2d.eventlogger.reports.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlMapperConfig;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoadObjects {
    @Autowired
    private JdbcTemplate template;
    @Autowired
    private SqlMapperConfig configMapper;

    public List<LoggableEvent> loadAllApiRequestEvent() {
        return getAll("event_api_request", ApiRequestEvent.class);
    }

    public List<LoggableEvent> loadAllApiResponseEvent() {
        return getAll("event_api_response", ApiResponseEvent.class);
     }

    public List<LoggableEvent> loadAllReloadEvent() {
        return getAll("event_bene_reload", ReloadEvent.class);
    }

    public List<LoggableEvent> loadAllContractBeneSearchEvent() {
        return getAll("event_bene_search", ContractBeneSearchEvent.class);
    }

    public List<LoggableEvent> loadAllErrorEvent() {
        return getAll("event_error", ErrorEvent.class);
    }

    public List<LoggableEvent> loadAllFileEvent() {
        return getAll("event_file", FileEvent.class);
    }

    public List<LoggableEvent> loadAllJobStatusChangeEvent() {
        return getAll("event_job_status_change", JobStatusChangeEvent.class);
    }

    public List<LoggableEvent> loadAllBeneficiarySearchEvent() {
        return null;
    }

    private List<LoggableEvent> getAll(String table, Class eventClass) {
        String qry = "SELECT * FROM " + table;
        return template.query(qry, configMapper.getMapper(eventClass));
    }
}
