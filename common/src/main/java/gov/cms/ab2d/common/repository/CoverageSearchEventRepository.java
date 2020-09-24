package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CoverageSearchEventRepository extends JpaRepository<CoverageSearchEvent, Long> {

    /*
     * Attempted to JPQL to make the below query but ran into issues with the limit and offset.
     *
     * You can use Sortable to potentially solve those issues but it doesn't seem worth it
     */

    /**
     * Get a single IN_PROGRESS search event from the past
     * @param searchId id of a coverage period identifying a contractId, month, year triple
     * @param offset offset in search basically get nth result
     * @return search event found at offset
     */
    @Query(value = "SELECT * FROM event_bene_coverage_search_status_change as ebc " +
            "   WHERE ebc.bene_coverage_period_id = :searchId AND ebc.new_status = 'IN_PROGRESS'" +
            "   ORDER BY ebc.created DESC LIMIT 1 OFFSET :offset",
            nativeQuery = true)
    Optional<CoverageSearchEvent> findSearchWithOffset(int searchId, int offset);

    Optional<CoverageSearchEvent> findFirstByCoveragePeriodOrderByCreatedDesc(CoveragePeriod period);
}
