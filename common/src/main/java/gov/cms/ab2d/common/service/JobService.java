package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Job;
import org.springframework.core.io.Resource;

import java.net.MalformedURLException;

public interface JobService {

    Job createJob(String resourceTypes, String url);

    void cancelJob(String jobId);

    Job getJobByJobId(String jobId);

    Job updateJob(Job job);

    Resource getResourceForJob(String jobID, String fileName) throws MalformedURLException;
}
