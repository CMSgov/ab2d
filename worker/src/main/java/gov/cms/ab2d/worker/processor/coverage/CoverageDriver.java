package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.CoveragePeriod;

public interface CoverageDriver {

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
}
