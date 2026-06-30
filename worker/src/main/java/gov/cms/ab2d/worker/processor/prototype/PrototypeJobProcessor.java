package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;

public interface PrototypeJobProcessor {

    Job process(String jobUuid);
}
