package gov.cms.ab2d.worker.processor;

/**
 * Write only interface for the channel implementation to update jobs.
 */
public interface JobProgressUpdateService {
    boolean addMeasure(String jobUuid, JobMeasure measure, long value);

    /*
     * A hack to prime the implementation with a jobguid in order to avoid dealing with distributed computing issues.
     * We take advantage of the fact that the actual tracking is done in the same VM as the worker.
     *
     * If the JopProgress is ever deployed as it's own microservice, then this method goes away and another technique
     * would need ot be used to insure consistency.
     */
    void initJob(String jobUuid);
}
