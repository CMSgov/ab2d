package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;

public interface PrototypeJobProcessor {

    Job process(String jobUuid);

    /**
     * Gracefully stop any running prototype batch executions and wait for their partition
     * threads to finish.
     */
    void stopForShutdown();
}
