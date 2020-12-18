package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.FeatureEngagement;

public interface WorkerService {

    void process(Job job);

    FeatureEngagement getEngagement();
}
