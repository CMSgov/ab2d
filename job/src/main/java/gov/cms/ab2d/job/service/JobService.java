package gov.cms.ab2d.job.service;

import gov.cms.ab2d.job.dto.JobPollResult;
import gov.cms.ab2d.job.dto.StaleJob;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.job.model.Job;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

public interface JobService {

    Job createJob(StartJobDTO startJobDTO);

    void cancelJob(String jobUuid, String organization);

    Job getAuthorizedJobByJobUuid(String jobUuid, String organization);

    Job getJobByJobUuid(String jobUuid);

    Job updateJob(Job job);

    Resource getResourceForJob(String jobUuid, String fileName, String organization) throws MalformedURLException;

    void incrementDownloadCount(File file, String jobUuid);

    int activeJobs(String organization);

    List<String> getActiveJobIds(String organization);

    JobPollResult poll(boolean admin, String jobUuid, String organization, int delaySeconds);

    List<StaleJob> checkForExpiration(List<String> jobUuids, int ttlHours);
}
