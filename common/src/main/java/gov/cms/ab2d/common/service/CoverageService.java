package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CoverageService {

    /**
     * Get {@link CoveragePeriod} in database matching provided triple
     * @param contract existing {@link gov.cms.ab2d.common.model.Contract}
     * @param month valid month
     * @param year valid year (not later than current year)
     */
    CoveragePeriod getCoveragePeriod(Contract contract, int month, int year);

    /**
     * Get {@link CoveragePeriod} in database matching provided triple
     * @param month valid month
     * @param year valid year (not later than current year)
     */
    List<CoveragePeriod> getCoveragePeriods(int month, int year);

    /**
     * Create {@link CoveragePeriod} matching the provided triple
     * @param contract contract to add coverage period for
     * @param month month of the period
     * @param year year of the period
     * @return coverage period as it exists in database
     */
    CoveragePeriod getCreateIfAbsentCoveragePeriod(Contract contract, int month, int year);

    /**
     * Retrieve list of coverage periods associated with a contract
     * @param contract contract to retrieve coverage periods for
     * @return list of coverage periods associated with a contract
     */
    List<CoveragePeriod> findAssociatedCoveragePeriods(Contract contract);

    /**
     * Check current status of a {@link CoveragePeriod}
     * @param periodId {@link CoveragePeriod#getId()} of the relevant search
     * @return true if search {@link CoveragePeriod#getStatus()} is {@link JobStatus#IN_PROGRESS}
     */
    boolean isCoveragePeriodInProgress(int periodId);

    /**
     * Given a list of coverage periods count the number of distinct beneficiaries over all of those coverage periods
     * @param coveragePeriods list of coverage periods that should have enrollment
     * @return number of distinct beneficiaries found
     */
    int countBeneficiariesByCoveragePeriod(List<CoveragePeriod> coveragePeriods);

    /**
     * Get coverage count for each coverage period related to a list of contracts. If a contract/year/month is missing
     * from the summary it means that either the coverage period does not exist or there are no records for that
     * coverage period.
     *
     * @param contracts list of contracts to search for
     * @return a summary of the coverage records found for a contract/year/month
     */
    List<CoverageCount> countBeneficiariesForContracts(List<Contract> contracts);

    /**
     * Can an EOB search be started based on whether a contract mapping is in progress
     * @return true if search {@link CoveragePeriod#getStatus()} is not {@link JobStatus#IN_PROGRESS}
     */
    boolean canEOBSearchBeStarted(int periodId);

    /**
     * Get search status {@link JobStatus}
     * @param periodId {@link CoveragePeriod#getId()}
     * @return search status or null
     */
    JobStatus getSearchStatus(int periodId);

    /**
     * Find the last {@link CoverageSearchEvent} associated with a {@link CoveragePeriod}
     * @param periodId {@link CoveragePeriod#getId()}
     * @return may return empty if no events associated with search have been recorded yet
     */
    Optional<CoverageSearchEvent> findLastEvent(int periodId);

    /**
     * Insert new coverage information for beneficiaries
     * @param searchEventId {@link CoverageSearchEvent#getId()} for the specific search event being performed.
     * This is used specifically for auditing and gauging the effect of
     * @param beneficiaryIds list of beneficiaries for this coverage period and this specific search event
     * @return relevant
     */
    CoverageSearchEvent insertCoverage(long searchEventId, Set<Identifiers> beneficiaryIds);

    /**
     * Delete all data from previous coverage search conducted for a given {@link CoveragePeriod}.
     *
     * Finds all {@link CoverageSearchEvent}s that have the {@link CoverageSearchEvent#getNewStatus()} as {@link JobStatus#IN_PROGRESS}
     * and deletes the second most recent one.
     *
     * @param periodId {@link CoveragePeriod#getId()}
     */
    void deletePreviousSearch(int periodId);

    /**
     * Pull coverage information for the given page and pageSize number of beneficiaries.
     *
     * If the page size is 1000 then the first page will get all information for beneficiaries 0 - 999
     * for the provided coverage periods.
     *
     * @param pagingRequest with details of page including page size, cursor, and coverage periods
     * @return coverage summary for page of beneficiaries and a pre-formatted request for the next set of beneficiaries
     */
    CoveragePagingResult pageCoverage(CoveragePagingRequest pagingRequest);

    /**
     * Get difference in beneficiary membership between last two searches conducted for a given coverage search
     * @param periodId the search period to find the last two searches for
     * @return difference between the two searches
     */
    CoverageSearchDiff searchDiff(int periodId);

    /**
     * Find all coverage periods that have never been searched
     */
    List<CoveragePeriod> coveragePeriodNeverSearchedSuccessfully();

    /**
     * Find all coverage periods for a given month since
     * @param month month to search
     * @param year year to search
     * @param lastSuccessful last search that successfully completed
     * @return matching coverage periods
     */
    List<CoveragePeriod> coveragePeriodNotUpdatedSince(int month, int year, OffsetDateTime lastSuccessful);

    /**
     * Find jobs that have been in progress for too long. This catches when jobs crash
     * or fail and the status is not updated to failed.
     * @param startedBefore started before
     * @return coverage periods with jobs stuck in progress
     */
    List<CoveragePeriod> coveragePeriodStuckJobs(OffsetDateTime startedBefore);

    /**
     * Change a coverage search to {@link JobStatus#SUBMITTED} and log an event.
     *
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is {@link JobStatus#IN_PROGRESS} already
     */
    Optional<CoverageSearchEvent> submitSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#SUBMITTED} and log an event.
     * @param periodId unique id of a coverage search
     * @param attempts number of attempts already conducted
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is {@link JobStatus#IN_PROGRESS} already
     */
    Optional<CoverageSearchEvent> submitSearch(int periodId, int attempts, String description);

    /**
     * Change a coverage search to {@link JobStatus#SUBMITTED}, log an event, and make sure this search is given high
     * priority to execute as soon as possible
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is {@link JobStatus#IN_PROGRESS} already
     */
    Optional<CoverageSearchEvent> prioritizeSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#SUBMITTED}, log an event, and make sure this search is given high
     * priority to execute as soon as possible
     * @param periodId unique id of a coverage search
     * @param attempts number of attempts already conducted
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is {@link JobStatus#IN_PROGRESS} already
     */
    Optional<CoverageSearchEvent> prioritizeSearch(int periodId, int attempts, String description);

    /**
     * Find next coverage search to start, change coverage search to {@link JobStatus#IN_PROGRESS}, and log an event.
     * @param description reason or explanation for change
     * @return resulting coverage search event if there is a search in the queu
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#SUBMITTED} state when this job is received
     */
    Optional<CoverageMapping> startSearch(CoverageSearch search, String description);

    /**
     * Resubmit a search that has failed but still has attempts
     * @param periodId unique id of a coverage search
     * @param attempts number of attempts already conducted
     * @param failedDescription reason or explanation for change
     * @param restartDescription reason or explanation for change
     * @param prioritize whether to force coverage period to front of the queue
     * @return resulting coverage search event
     */
    CoverageSearchEvent resubmitSearch(int periodId, int attempts, String failedDescription,
                                       String restartDescription, boolean prioritize);

    /**
     * Change a coverage search to {@link JobStatus#CANCELLED} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#SUBMITTED} or {@link JobStatus#IN_PROGRESS}
     * state when this job is received
     */
    CoverageSearchEvent cancelSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#FAILED} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not {@link JobStatus#SUBMITTED} or {@link JobStatus#IN_PROGRESS}
     * state when this job is received
     */
    CoverageSearchEvent failSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#SUCCESSFUL} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#IN_PROGRESS} state when this job is received
     */
    CoverageSearchEvent completeSearch(int periodId, String description);
}
