package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.worker.processor.domainmodel.CoverageMapping;

import java.util.Collection;

public interface CoverageProcessor {

    //          1 Parent Processor
    //              - finding all out of date coverage periods and queueing jobs
    //              - method for inserting coverage information into the database
    //                      BlockingQueue<CoverageMapping> insertionQueue
    //              - wait to start jobs until there is guaranteed to be enough memory when those jobs return

    // Stop processing jobs from the queue when the length of the results queue is greater than the number of threads
    // insertionQueue.size() > NUM_THREADS -> stop

    // Configure the number of months to go back
    // Quartz/Cron job
    // Find all (with null status) or (within x months in the past and with last successful search before (now - f(x) => returns x * CONSTANT)))
    // Constantly blocked by running jobs and never able to get them
    // x months in the properties service

    /**
     * Check database for all {@link CoveragePeriod} that are missing information completely
     * or the last successful search {@link gov.cms.ab2d.common.model.CoverageSearchEvent} is too
     * long ago and makes the search stale.
     *
     * Only searches for stale searches at a configured number of months into the past.
     */
    void queueStaleCoveragePeriods();


    /**
     * Add a {@link CoveragePeriod} to the queue of periods to be mapped
     * @param period coverage period to add
     * @param prioritize if true insert in front of the queue
     */
    void queueCoveragePeriod(CoveragePeriod period, boolean prioritize);

    /**
     * Add a collection of {@link CoveragePeriod}s to the queue of periods to be mapped
     * @param periods coverage period to add
     */
    void queueCoveragePeriod(Collection<CoveragePeriod> periods);

    // Crisis big red button self destruct
    // Scheduled check for shutdown
    // Run loop
    void shutdown();
}
