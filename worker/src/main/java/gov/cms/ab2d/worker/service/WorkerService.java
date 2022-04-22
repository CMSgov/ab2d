package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.job.model.Job;

public interface WorkerService {

    Job process(String jobId);

    FeatureEngagement getEngagement();
}
