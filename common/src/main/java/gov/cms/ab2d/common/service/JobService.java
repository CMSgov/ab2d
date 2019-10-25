package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Job;

public interface JobService {

    Job createJob(String resourceTypes, String url);

    void cancelJob(String jobId);

    Job getJobByJobID(String jobId);

    Job updateJob(Job job);
}
