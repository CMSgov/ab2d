package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.JobPollResult;
import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.dto.StartJobDTO;
import gov.cms.ab2d.common.model.Job;
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

    void deleteFileForJob(File file, String jobUuid);

    int activeJobs(String organization);

    List<String> getActiveJobIds(String organization);

    JobPollResult poll(boolean admin, String jobUuid, String organization, int delaySeconds);

    List<StaleJob> checkForExpiration(List<String> jobUuids, int ttlHours);
}
