package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.job.model.Job;

public interface JobPreProcessor {

    Job preprocess(String jobUuid);

}
