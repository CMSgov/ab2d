package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;

/**
 * Provide an interface for executing high level actions concerning enrollment.
 *
 * Encompasses
 *      - Discovering and queueing coverage periods that need to be updated
 *      - Determining how many beneficiaries an EOB job should expect to query from BFD based on the number
 *          of beneficiaries in the database (used to detect bugs).
 *      - Determining whether all enrollment necessary to run a Job for a Contract is present
 *          in the database. Checks that all {@link gov.cms.ab2d.common.model.CoveragePeriod}s
 *          expected for a contract are present, that updates to the coverage associated with those coverage periods
 *          are not in progress, and that updates have not failed recently.
 *      - Retrieving all enrollment/coverage for an EOB job
 *      - Verifying that enrollment/coverage meets business requirements
 */
public interface CoverageDriver {

    /**
     * Check database for all {@link gov.cms.ab2d.common.model.CoveragePeriod} that are missing information completely
     * or the last successful search {@link gov.cms.ab2d.coverage.model.CoverageSearchEvent} is too
     * long ago and makes the search stale.
     *
     * Only searches for stale searches at a configured number of months into the past.
     *
     * @throws CoverageDriverException if  lock cannot be obtained within timeout
     * @throws InterruptedException if thread is interrupted trying to obtain lock
     */
    void queueStaleCoveragePeriods() throws InterruptedException;

    /**
     * Check all {@link gov.cms.ab2d.common.model.Contract} for attestation dates and create {@link gov.cms.ab2d.common.model.CoveragePeriod}s
     * for all months since the attestation of those contracts.
     *
     * @throws CoverageDriverException if  lock cannot be obtained within timeout
     * @throws InterruptedException if thread is interrupted trying to obtain lock
     */
    void discoverCoveragePeriods() throws InterruptedException;

    /**
     * Check whether all metadata necessary for conducting a an eob {@link Job} is present or not.
     * If not, queue necessary coverage search jobs.
     * @param job job to check for coverage
     * @return true if coverage mappings are available and false if not
     * @throws CoverageDriverException if unexpected behavior causes
     * @throws InterruptedException if thread is interrupted trying to obtain lock
     */
    boolean isCoverageAvailable(Job job) throws InterruptedException;

    /**
     * Calculate the number of beneficiaries to process for a given job
     * @param job an already submitted eob job
     * @return the total number of beneficiaries to search for the job which equals the number of eob searches necessary
     */
    int numberOfBeneficiariesToProcess(Job job);

    /**
     * Get first page worth of beneficiaries to run EOB searches on
     * @param job eob job to find first page for
     * @return result containing first page of beneficiaries and request to get next page if more beneficiaries are present
     */
    CoveragePagingResult pageCoverage(Job job);

    /**
     * Get a page of beneficiaries based on the provided request. If a cursor is provided use the cursor, otherwise
     * start with first page of beneficiaries.
     * @param request cursor pointing to starting point of next set of beneficiaries
     * @return result containing page of beneficiaries and request to get next page if more beneficiaries are present
     */
    CoveragePagingResult pageCoverage(CoveragePagingRequest request);

    /**
     * Verify that the coverage information in the database meets all business requirements.
     *
     * This method is called by {@link gov.cms.ab2d.worker.quartz.CoverageCheckQuartzJob} periodically.
     *
     * @throws CoverageVerificationException if verification fails for at least one contract
     */
    void verifyCoverage();
}
