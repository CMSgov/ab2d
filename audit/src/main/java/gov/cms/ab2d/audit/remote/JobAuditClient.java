package gov.cms.ab2d.audit.remote;

import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobAuditClient {
    private final JobService jobService;

    @Autowired
    public JobAuditClient(JobService jobService) {
        this.jobService = jobService;
    }

    public List<StaleJob> checkForExpiration(List<String> jobUuids, int ttlHours) {
        return jobService.checkForExpiration(jobUuids, ttlHours);
    }
}
