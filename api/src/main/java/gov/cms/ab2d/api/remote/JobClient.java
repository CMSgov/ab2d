package gov.cms.ab2d.api.remote;

import gov.cms.ab2d.common.dto.JobPollResult;
import gov.cms.ab2d.common.dto.StartJobDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

@Component
public class JobClient {

    private final JobService jobService;

    @Autowired
    public JobClient(JobService jobService) {
        this.jobService = jobService;
    }

    // Returns job_guid
    public String createJob(StartJobDTO startJobDTO) {
        Job job = jobService.createJob(startJobDTO);
        return job.getJobUuid();
    }

    public int activeJobs(String organization) {
        return jobService.activeJobs(organization);
    }

    public List<String> getActiveJobIds(String organization) {
        return jobService.getActiveJobIds(organization);
    }

    public Resource getResourceForJob(String jobGuid, String fileName, String organization) {
        try {
            return jobService.getResourceForJob(jobGuid, fileName, organization);
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue);  // NOSONAR
        }
    }

    public JobPollResult poll(boolean admin, String jobUuid, String organization, int delaySeconds) {
        return jobService.poll(admin, jobUuid, organization, delaySeconds);
    }

    public void cancelJob(String jobUuid, String organization) {
        jobService.cancelJob(jobUuid, organization);
    }
}
