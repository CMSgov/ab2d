package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;

public interface JobPreProcessor {

    Job preprocess(String jobUuid);

}
