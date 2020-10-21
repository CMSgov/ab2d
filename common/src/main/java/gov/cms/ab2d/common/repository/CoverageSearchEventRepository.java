package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CoverageSearchEventRepository extends JpaRepository<CoverageSearchEvent, Long> {

    /*
     * Attempted to JPQL to make the below query but ran into issues with the limit and offset.
     *
     * You can use Sortable to potentially solve those issues but it doesn't seem worth it
     */

    /**
     * Get a single IN_PROGRESS search event from the past
     * @param periodId id of a coverage period identifying a contractId, month, year triple
     * @param offset offset in search basically get nth result
     * @return search event found at offset
     */
    @Query(value = "SELECT * FROM event_bene_coverage_search_status_change as ebc " +
            "   WHERE ebc.bene_coverage_period_id = :periodId AND ebc.new_status = :status" +
            "   ORDER BY ebc.created DESC LIMIT 1 OFFSET :offset",
            nativeQuery = true)
    Optional<CoverageSearchEvent> findSearchEventWithOffset(int periodId, String status, int offset);

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
