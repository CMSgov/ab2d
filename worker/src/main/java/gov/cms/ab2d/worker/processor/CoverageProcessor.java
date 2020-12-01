package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoveragePeriod;

public interface CoverageProcessor {

    /**
     * Check database for all {@link CoveragePeriod} that are missing information completely
     * or the last successful search {@link gov.cms.ab2d.common.model.CoverageSearchEvent} is too
     * long ago and makes the search stale.
     *
     * Only searches for stale searches at a configured number of months into the past.
     */
    void queueStaleCoveragePeriods();

    /**
     * Check all {@link gov.cms.ab2d.common.model.Contract} for attestation dates and create {@link CoveragePeriod}s
     * for all months since the attestation of those contracts.
     */
    void discoverCoveragePeriods();

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

    // Crisis big red button self destruct
    // Scheduled check for shutdown
    // Run loop
    void shutdown();
}
