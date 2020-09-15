package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.JobStatus;

import java.util.Collection;
import java.util.Optional;

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
     * Find the last {@link CoverageSearchEvent} associated with a {@link CoveragePeriod}
     * @param periodId {@link CoveragePeriod#getId()} ()}
     * @return search
     */
    CoverageSearchEvent getLastEvent(int periodId);

    /**
     * Insert new coverage information for beneficiaries
     * @param periodId {@link CoveragePeriod#getId()}
     * @param searchEventId {@link CoverageSearchEvent#getId()} for the specific search event being performed.
     * This is used specifically for auditing and gauging the effect of
     * @param beneficiaryIds list of beneficiaries for this coverage period and this specific search event
     * @return relevant
     */
    CoverageSearchEvent insertCoverage(int periodId, long searchEventId, Collection<String> beneficiaryIds);

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
     * Get difference in beneficiary membership between last two searches conducted for a given coverage search
     * @param periodId
     * @return
     */
    CoverageSearchDiff searchDiff(int periodId);

    /**
     * Change a coverage search to {@link JobStatus#SUBMITTED} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is {@link JobStatus#IN_PROGRESS} already
     */
    CoverageSearchEvent submitCoverageSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#IN_PROGRESS} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#SUBMITTED} state when this job is received
     */
    CoverageSearchEvent startCoverageSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#CANCELLED} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#SUBMITTED} or {@link JobStatus#IN_PROGRESS}
     * state when this job is received
     */
    CoverageSearchEvent cancelCoverageSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#FAILED} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not {@link JobStatus#SUBMITTED} or {@link JobStatus#IN_PROGRESS}
     * state when this job is received
     */
    CoverageSearchEvent failCoverageSearch(int periodId, String description);

    /**
     * Change a coverage search to {@link JobStatus#SUCCESSFUL} and log an event.
     * @param periodId unique id of a coverage search
     * @param description reason or explanation for change
     * @return resulting coverage search event
     * @throws InvalidJobStateTransition if job is not in the {@link JobStatus#IN_PROGRESS} state when this job is received
     */
    CoverageSearchEvent completeCoverageSearch(int periodId, String description);
}
