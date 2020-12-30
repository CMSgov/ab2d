package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.service.FeatureEngagement;

public interface WorkerService {

    boolean process(String jobId);

    FeatureEngagement getEngagement();
}
