package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CoverageSearchEventRepository extends JpaRepository<CoverageSearchEvent, Long> {

    /**
     * Find most recent events associated with a coverage search
     * @param periodId coverage period corresponding to contract, year, and month
     * @param limit number of events in the past to find
     * @return all events associated with coverage period
     */
    @Query(value = "SELECT * FROM event_bene_coverage_search_status_change as ebc " +
            "   WHERE ebc.bene_coverage_period_id = :periodId" +
            "   ORDER BY ebc.created DESC LIMIT :limit",
            nativeQuery = true)
    List<CoverageSearchEvent> findByPeriodDesc(int periodId, int limit);

    /**
     * Look for all jobs currently in the provided {@link gov.cms.ab2d.common.model.JobStatus} which have been in that status since
     * some point in time.
     *
     * Basically, if today is Tuesday and a job has been in the status IN_PROGRESS since Sunday, then the job
     * is "stuck" in its current status.
     * @param status status to search for
     * @param since point in time that a job that we are checking status of job against
     * @return list of {@link CoverageSearchEvent} that are stuck in the provided status
     */
    @Query(value = "SELECT * FROM ( " +
            "                  SELECT DISTINCT ON (cov_event.bene_coverage_period_id) cov_event.* " +
            "                  FROM event_bene_coverage_search_status_change cov_event " +
            "                  ORDER BY cov_event.bene_coverage_period_id, cov_event.created DESC " +
            "              ) as latest " +
            "       WHERE latest.new_status = :status AND latest.created < :since ", nativeQuery = true)
    List<CoverageSearchEvent> findStuckAtStatus(String status, OffsetDateTime since);

    Optional<CoverageSearchEvent> findFirstByCoveragePeriodOrderByCreatedDesc(CoveragePeriod period);
}
