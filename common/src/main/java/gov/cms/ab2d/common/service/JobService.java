package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.fhir.FhirVersion;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;

public interface JobService {
    String ZIPFORMAT = "application/zip";

    Job createJob(String resourceTypes, String url, String contractNumber, String outputFormat, OffsetDateTime since, FhirVersion version);

    void cancelJob(String jobUuid);

    Job getAuthorizedJobByJobUuidAndRole(String jobUuid);

    Job getJobByJobUuid(String jobUuid);

    Job updateJob(Job job);

    Resource getResourceForJob(String jobUuid, String fileName) throws MalformedURLException;

    void deleteFileForJob(File file, String jobUuid);

    boolean checkIfCurrentClientCanAddJob();
}
