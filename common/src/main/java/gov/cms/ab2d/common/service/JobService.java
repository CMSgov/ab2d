package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Job;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;

public interface JobService {
    String ZIPFORMAT = "application/zip";

    // todo: get rid of the contractNumber as an argument
    Job createJob(String resourceTypes, String url, @Deprecated String contractNumber, String outputFormat, OffsetDateTime since);

    void cancelJob(String jobUuid);

    Job getAuthorizedJobByJobUuidAndRole(String jobUuid);

    Job getJobByJobUuid(String jobUuid);

    Job updateJob(Job job);

    Resource getResourceForJob(String jobUuid, String fileName) throws MalformedURLException;

    void deleteFileForJob(File file, String jobUuid);

    boolean checkIfCurrentUserCanAddJob();
}
