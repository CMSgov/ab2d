package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;

import java.util.List;
import java.util.Map;

public interface JobOutputService {

    JobOutput updateJobOutput(JobOutput jobOutput);

    JobOutput findByFilePathAndJob(String filePath, Job job);

    Map<StaleJob, List<String>>  expiredDownloadableFiles(int minutesInterval);
}
