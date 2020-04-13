package gov.cms.ab2d.eventlogger.reports.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeleteObjects {
    @Autowired
    private JdbcTemplate template;

    public void deleteAllApiRequestEvent() {
        deleteAll("event_api_request");
    }

    public void deleteAllApiResponseEvent() {
        deleteAll("event_api_response");
     }

    public void deleteAllReloadEvent() {
        deleteAll("event_bene_reload");
    }

    public void deleteAllContractBeneSearchEvent() {
        deleteAll("event_bene_search");
    }

    public void deleteAllErrorEvent() {
        deleteAll("event_error");
    }

    public void deleteAllFileEvent() {
        deleteAll("event_file");
    }

    public void deleteAllJobStatusChangeEvent() {
        deleteAll("event_job_status_change");
    }

    private void deleteAll(String table) {
        String qry = "DELETE FROM " + table;
        template.update(qry);
    }
}
