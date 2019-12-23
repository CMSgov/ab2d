package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;

public interface JobProcessor {

    Job process(String jobUuid);
}
