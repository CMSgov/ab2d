package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;

import java.util.Collection;
import java.util.Optional;

public interface CoverageService {

    /**
     * Get {@link CoverageSearch} in database matching provided triple
     * @param contractId existing {@link Contract#getId()}
     * @param month valid month
     * @param year valid year (not later than current year)
     */
    CoverageSearch getCoverageSearch(long contractId, int month, int year);

    /**
     * Check current status of a {@link CoverageSearch}
     * @param searchId {@link CoverageSearch#getId()} of the relevant search
     * @return true if search {@link CoverageSearch#getStatus()} is {@link JobStatus#IN_PROGRESS}
     */
    boolean isCoverageSearchInProgress(int searchId);

    /**
     * Can an EOB search be started based on whether a contract mapping is in progress
     * @return true if search {@link CoverageSearch#getStatus()} is not {@link JobStatus#IN_PROGRESS}
     */
    boolean canEOBSearchBeStarted(int searchId);

    /**
     * Get search status {@link JobStatus}
     * @param searchId {@link CoverageSearch#getId()}
     * @return search status or null
     */
    JobStatus getSearchStatus(int searchId);

    /**
     * Find the last {@link CoverageSearchEvent} associated with a {@link CoverageSearch}
     * @param searchId {@link CoverageSearch#getId()}
     * @return may return empty if no events associated with search have been recorded yet
     */
    Optional<CoverageSearchEvent> findLastEvent(int searchId);

    /**
     * Find the last {@link CoverageSearchEvent} associated with a {@link CoverageSearch}
     * @param searchId {@link CoverageSearch#getId()}
     * @return search
     */
    CoverageSearchEvent getLastEvent(int searchId);

    /**
     * Insert new coverage information for beneficiaries
     * @param searchId {@link CoverageSearch#getId()}
     * @param searchEventId {@link CoverageSearchEvent#getId()} for the specific search event being performed.
     * This is used specifically for auditing and gauging the effect of
     * @param beneficiaryIds list of beneficiaries for this coverage period and this specific search event
     * @return relevant
     */
    CoverageSearchEvent insertCoverage(int searchId, long searchEventId, Collection<String> beneficiaryIds);

    /**
     * Delete all data from previous coverage search conducted for a given {@link CoverageSearch}.
     *
     * Finds all {@link CoverageSearchEvent}s that have the {@link CoverageSearchEvent#getNewStatus()} as {@link JobStatus#IN_PROGRESS}
     * and deletes the second most recent one.
     *
     * @param searchId {@link CoverageSearch#getId()}
     */
    void deletePreviousSearch(int searchId);

    /**
     * Get difference in beneficiary membership between last two searches conducted for a given coverage search
     * @param searchId
     * @return
     */
    CoverageSearchDiff searchDiff(int searchId);

    /**
     * Change a coverage search to {@link JobStatus#SUBMITTED} and log an event.
     * @param searchId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is {@link JobStatus#IN_PROGRESS} already
     */
    CoverageSearchEvent submitCoverageSearch(int searchId, String description);

    /**
     * Change a coverage search to {@link JobStatus#IN_PROGRESS} and log an event.
     * @param searchId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#SUBMITTED} state when this job is received
     */
    CoverageSearchEvent startCoverageSearch(int searchId, String description);

    /**
     * Change a coverage search to {@link JobStatus#CANCELLED} and log an event.
     * @param searchId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#SUBMITTED} or {@link JobStatus#IN_PROGRESS}
     * state when this job is received
     */
    CoverageSearchEvent cancelCoverageSearch(int searchId, String description);

    /**
     * Change a coverage search to {@link JobStatus#FAILED} and log an event.
     * @param searchId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not {@link JobStatus#SUBMITTED} or {@link JobStatus#IN_PROGRESS}
     * state when this job is received
     */
    CoverageSearchEvent failCoverageSearch(int searchId, String description);

    /**
     * Change a coverage search to {@link JobStatus#SUCCESSFUL} and log an event.
     * @param searchId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#IN_PROGRESS} state when this job is received
     */
    CoverageSearchEvent completeCoverageSearch(int searchId, String description);
}
