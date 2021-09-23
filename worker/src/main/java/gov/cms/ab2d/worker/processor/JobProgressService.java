package gov.cms.ab2d.worker.processor;

/**
 * Read only API for fetching the current status of a job.
 */
public interface JobProgressService {

    ProgressTracker getStatus(String jobUuid);
}
