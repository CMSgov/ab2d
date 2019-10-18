package gov.cms.ab2d.api.service;

import gov.cms.ab2d.domain.Job;

public interface JobService {

    Job createJob(String resourceTypes, String url);

    void cancelJob(String jobId);
}
