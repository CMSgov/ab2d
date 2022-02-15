package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.StartJobDTO;
import gov.cms.ab2d.common.model.Job;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

public interface JobService {
    String ZIPFORMAT = "application/zip";

    Job createJob(StartJobDTO startJobDTO);

    void cancelJob(String jobUuid);

    Job getAuthorizedJobByJobUuidAndRole(String jobUuid);

    Job getJobByJobUuid(String jobUuid);

    Job updateJob(Job job);

    Resource getResourceForJob(String jobUuid, String fileName) throws MalformedURLException;

    void deleteFileForJob(File file, String jobUuid);

    boolean checkIfCurrentClientCanAddJob();

    List<String> getActiveJobIds();
}
