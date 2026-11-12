package gov.cms.ab2d.worker.processor.pauseresume;

import gov.cms.ab2d.job.model.Job;

import java.util.Set;

public interface PauseResumeJobProcessor {

    Job process(String jobUuid);

    /**
     * Gracefully stop any running pause/resume batch executions and wait for their partition
     * threads to finish.
     */
    void stopForShutdown(Set<String> ownedJobs);

    /**
     * Signal pause/resume jobs to stop
     */
    void stopRunning(Set<String> ownedJobs);
}
