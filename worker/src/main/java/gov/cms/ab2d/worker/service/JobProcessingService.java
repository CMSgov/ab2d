package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;

public interface JobProcessingService {

    void putJobInProgress(String jobId);

    Job processJob(String jobId);

    void completeJob(Job job);
}
