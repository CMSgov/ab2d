package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.PropertyConstants;
import gov.cms.ab2d.job.service.JobService;

public class WorkerServiceStub implements WorkerService {

    private final JobService jobService;
    private final PropertiesService propertiesService;
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
        return FeatureEngagement.fromString(propertiesService.getProperty(PropertyConstants.WORKER_ENGAGEMENT, FeatureEngagement.IN_GEAR.getSerialValue()));
    }
}
