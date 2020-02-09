package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Job;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.MalformedURLException;

public interface JobService {

    Job createJob(String resourceTypes, String url, String outputFormat);

    Job createJob(String resourceTypes, String url, String contractNumber, String outputFormat);

    void cancelJob(String jobUuid);

    Job getJobByJobUuid(String jobUuid);

    Job updateJob(Job job);

    Resource getResourceForJob(String jobUuid, String fileName) throws MalformedURLException;

    void deleteFileForJob(File file);

    boolean checkIfCurrentUserCanAddJob();
}
