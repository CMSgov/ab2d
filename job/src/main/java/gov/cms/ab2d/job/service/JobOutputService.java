package gov.cms.ab2d.job.service;

import gov.cms.ab2d.job.dto.StaleJob;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;

import java.util.List;
import java.util.Map;

public interface JobOutputService {

    JobOutput updateJobOutput(JobOutput jobOutput);

    JobOutput findByFilePathAndJob(String filePath, Job job);

    Map<StaleJob, List<String>>  expiredDownloadableFiles(int minutesInterval);
}
