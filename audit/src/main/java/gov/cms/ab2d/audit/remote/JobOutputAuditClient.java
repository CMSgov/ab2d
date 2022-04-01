package gov.cms.ab2d.audit.remote;

import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.service.JobOutputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JobOutputAuditClient {
    private final JobOutputService jobOutputService;

    @Autowired
    public JobOutputAuditClient(JobOutputService jobOutputService) {
        this.jobOutputService = jobOutputService;
    }

    public Map<StaleJob, List<String>> checkForDownloadedExpiredFiles(int interval) {
        return jobOutputService.expiredDownloadableFiles(interval);
    }
}
