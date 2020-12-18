package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;

public class WorkerServiceStub implements WorkerService {

    private JobService jobService;
    private PropertiesService propertiesService;
    public int processingCalls = 0;

    public WorkerServiceStub(JobService jobService, PropertiesService propertiesService) {
        this.jobService = jobService;
        this.propertiesService = propertiesService;
    }

    @Override
    public void process(Job job) {
        processingCalls += 1;
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setStatusMessage(null);
        jobService.updateJob(job);
    }

    @Override
    public FeatureEngagement getEngagement() {
        return FeatureEngagement.fromString(propertiesService.getPropertiesByKey(Constants.WORKER_ENGAGEMENT).getValue());
    }
}
