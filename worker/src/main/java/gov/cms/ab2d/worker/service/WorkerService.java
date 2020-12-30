package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.FeatureEngagement;

public interface WorkerService {

    Job process(String jobId);

    FeatureEngagement getEngagement();
}
