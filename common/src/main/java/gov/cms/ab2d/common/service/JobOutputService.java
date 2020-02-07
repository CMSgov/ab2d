package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.JobOutput;

public interface JobOutputService {

    JobOutput updateJobOutput(JobOutput jobOutput);

    JobOutput findByFilePath(String fileName);
}
