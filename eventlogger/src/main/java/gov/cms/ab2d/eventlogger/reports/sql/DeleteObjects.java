package gov.cms.ab2d.eventlogger.reports.sql;

import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlMapperConfig;
import gov.cms.ab2d.eventlogger.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeleteObjects {
    @Autowired
    private JdbcTemplate template;

    @Autowired
    private SqlMapperConfig configMapper;

    public void deleteAllApiRequestEvent() {
        deleteAll("event_api_request", ApiRequestEvent.class);
    }

    public void deleteAllApiResponseEvent() {
        deleteAll("event_api_response", ApiResponseEvent.class);
     }

    public void deleteAllReloadEvent() {
        deleteAll("event_bene_reload", ReloadEvent.class);
    }

    public void deleteAllContractBeneSearchEvent() {
        deleteAll("event_bene_search", ContractBeneSearchEvent.class);
    }

    public void deleteAllErrorEvent() {
        deleteAll("event_error", ErrorEvent.class);
    }

    public void deleteAllFileEvent() {
        deleteAll("event_file", FileEvent.class);
    }

    public void deleteAllJobStatusChangeEvent() {
        deleteAll("event_job_status_change", JobStatusChangeEvent.class);
    }

    private void deleteAll(String table, Class eventClass) {
        String qry = "DELETE FROM " + table;
        template.update(qry);
    }
}
