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
    public Job process(String jobId) {
        processingCalls += 1;
        Job job = jobService.getJobByJobUuid(jobId);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setStatusMessage(null);
        return jobService.updateJob(job);
    }

    @Override
    public FeatureEngagement getEngagement() {
        return FeatureEngagement.fromString(propertiesService.getPropertiesByKey(Constants.WORKER_ENGAGEMENT).getValue());
    }
}
