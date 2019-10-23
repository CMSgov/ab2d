package gov.cms.ab2d.api.service;

import gov.cms.ab2d.domain.Job;

public interface JobService {

    Job createJob(String resourceTypes, String url);

    Job getJobByJobID(String jobId);

    Job updateJob(Job job);
}
