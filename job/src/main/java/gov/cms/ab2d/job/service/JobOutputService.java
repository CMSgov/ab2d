package gov.cms.ab2d.job.service;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;

public interface JobOutputService {

    JobOutput updateJobOutput(JobOutput jobOutput);

    JobOutput findByFilePathAndJob(String filePath, Job job);
}
