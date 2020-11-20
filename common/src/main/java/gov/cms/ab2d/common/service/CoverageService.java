package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.common.model.JobStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CoverageService {

    /**
     * Get {@link CoveragePeriod} in database matching provided triple
     * @param contractId existing {@link gov.cms.ab2d.common.model.Contract#getId()}
     * @param month valid month
     * @param year valid year (not later than current year)
     */
    CoveragePeriod getCoveragePeriod(long contractId, int month, int year);

    /**
     * Check current status of a {@link CoveragePeriod}
     * @param periodId {@link CoveragePeriod#getId()} of the relevant search
     * @return true if search {@link CoveragePeriod#getStatus()} is {@link JobStatus#IN_PROGRESS}
     */
    boolean isCoveragePeriodInProgress(int periodId);

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
     * If the page size is 1000 then the first page will get records 0 - 999, 7th page will get records 6000 - 6999
     * @param pageNumber page through results by pageSize records at a time.
     * @param pageSize max number of beneficiaries in each page
     * @param coveragePeriods list of ids of coverage periods to search over
     * @return coverage summary for page of beneficiaries
     */
    List<CoverageSummary> pageCoverage(int pageNumber, int pageSize, List<Integer> coveragePeriods);

    /**
     * Pull coverage information for the given page and pageSize number of beneficiaries.
     *
     * If the page size is 1000 then the first page will get records 0 - 999, 7th page will get records 6000 - 6999
     * @param pageNumber page through results by pageSize records at a time.
     * @param pageSize max number of beneficiaries in each page
     * @param coveragePeriods list of ids of coverage periods to search over
     * @return coverage summary for page of beneficiaries
     */
    List<CoverageSummary> pageCoverage(int pageNumber, int pageSize, Integer... coveragePeriods);

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
