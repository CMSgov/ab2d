package gov.cms.ab2d.worker.processor;

public interface JobProgressService {

    void addMeasure(String jobId, JobMeasure measure, long value);

    ProgressTracker getStatus(String jobId);

}
