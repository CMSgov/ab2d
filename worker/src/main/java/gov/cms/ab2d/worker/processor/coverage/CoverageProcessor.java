package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;

public interface CoverageProcessor {

    /**
     * Add a {@link CoveragePeriod} to the queue of periods to be mapped
     * @param period coverage period to add
     * @param prioritize if true insert in front of the queue
     */
    void queueCoveragePeriod(CoveragePeriod period, boolean prioritize);

    /**
     * Add a coverage period to the list of periods to be searched
     * @param period period to be queued
     * @param attempts number of previous attempts made to fill in coverage information
     * @param prioritize if true place at front of queue
     */
    void queueCoveragePeriod(CoveragePeriod period, int attempts, boolean prioritize);

    /**
     * Queue a coverage period that failed or was cancelled for any reason
     * @param coverageMapping coverage mapping that failed
     * @param prioritize true if coverage mapping needs to be run first before other periods
     */
    void queueMapping(CoverageMapping coverageMapping, boolean prioritize);

    /**
     * Check if processor can accept a new job or whether it is busy
     * @return true if executor has room for another job
     */
    boolean isProcessorBusy();

    /**
     * Attempt to start a job, may fail if processor has been shutdown
     * @param coverageMapping abstraction representing job
     * @return true if job has been added to thread pool and is running, false otherwise
     */
    boolean startJob(CoverageMapping coverageMapping);

    // Crisis big red button self destruct
    // Scheduled check for shutdown
    // Run loop
    void shutdown();
}
