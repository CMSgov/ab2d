package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageMapping;

/**
 * Execute coverage searches against BFD and save the results to the database.
 *
 * This class does not trigger coverage searches, it only manages the execution of those searches.
 */
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
     * Check if processor can accept a new coverage period to search or if there are too many in progress searches
     * either currently querying BFD or inserting data into the AB2D database.
     *
     * @return if processor has available threads to run a job
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
