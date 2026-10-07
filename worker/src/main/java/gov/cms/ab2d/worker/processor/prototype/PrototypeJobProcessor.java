package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;

import java.util.Set;

public interface PrototypeJobProcessor {

    Job process(String jobUuid);

    /**
     * Gracefully stop any running prototype batch executions and wait for their partition
     * threads to finish.
     */
    void stopForShutdown(Set<String> ownedJobs);

    /**
     * Signal prototype jobs to stop
     */
    void stopRunning(Set<String> ownedJobs);
}
