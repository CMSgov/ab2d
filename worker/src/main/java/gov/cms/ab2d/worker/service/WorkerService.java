package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.service.WorkerDrive;

public interface WorkerService {

    void process(String jobId);

    WorkerDrive getEngagement();
}
