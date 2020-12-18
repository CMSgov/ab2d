package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.common.model.Job;

public interface JobPreProcessor {

    Job preprocess(Job job);

}
