package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobMeasure;

public interface JobChannelService {

    void sendUpdate(String jobId, JobMeasure measure, long value);
}
