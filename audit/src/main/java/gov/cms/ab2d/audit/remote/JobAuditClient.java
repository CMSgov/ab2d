package gov.cms.ab2d.audit.remote;

import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.service.JobOutputService;
import gov.cms.ab2d.common.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JobAuditClient {
    private final JobService jobService;
    private final JobOutputService jobOutputService;

    @Autowired
    public JobAuditClient(JobService jobService, JobOutputService jobOutputService) {
        this.jobService = jobService;
        this.jobOutputService = jobOutputService;
    }

    public List<StaleJob> checkForExpiration(List<String> jobUuids, int ttlHours) {
        return jobService.checkForExpiration(jobUuids, ttlHours);
    }

    public Map<StaleJob, List<String>> checkForOutputExpiration(int interval) {
        return jobOutputService.expiredDownloadableFiles(interval);
    }
}
