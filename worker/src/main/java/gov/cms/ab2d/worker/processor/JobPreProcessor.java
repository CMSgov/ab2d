package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;

public interface JobPreProcessor {

    Job preprocess(String jobUuid);

}
