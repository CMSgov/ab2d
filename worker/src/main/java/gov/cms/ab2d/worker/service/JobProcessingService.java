package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;

public interface JobProcessingService {

    Job putJobInProgress(String jobId);

    void completeJob(Job job);
}
