package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.Job;

public interface CoverageDriver {

    /**
     * Check database for all {@link CoveragePeriod} that are missing information completely
     * or the last successful search {@link gov.cms.ab2d.common.model.CoverageSearchEvent} is too
     * long ago and makes the search stale.
     *
     * Only searches for stale searches at a configured number of months into the past.
     */
    void queueStaleCoveragePeriods() throws CoverageDriverException, InterruptedException;

    /**
     * Check all {@link gov.cms.ab2d.common.model.Contract} for attestation dates and create {@link CoveragePeriod}s
     * for all months since the attestation of those contracts.
     */
    void discoverCoveragePeriods() throws CoverageDriverException, InterruptedException;

    /**
     * Check whether all metadata necessary for conducting a an eob {@link Job} is present or not.
     * If not, queue necessary coverage search jobs.
     * @param job job to check for coverage
     * @return true if coverage mappings are available and false if not
     * @throws CoverageDriverException on failure to retrieve database lock due to interruption
     */
    boolean isCoverageAvailable(Job job) throws CoverageDriverException;
}
