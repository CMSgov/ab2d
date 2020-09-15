package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CoverageSearchEventRepository extends JpaRepository<CoverageSearchEvent, Long> {

    @Query(value = "SELECT * FROM event_bene_coverage_search_status_change as ebc " +
            "   WHERE ebc.bene_coverage_period_id = ? AND ebc.new_status = 'IN_PROGRESS'" +
            "   ORDER BY ebc.created DESC LIMIT 1 OFFSET 1",
            nativeQuery = true)
    Optional<CoverageSearchEvent> findPreviousSearch(int searchId);

    @Query(value = "SELECT * FROM event_bene_coverage_search_status_change as ebc " +
            "   WHERE ebc.bene_coverage_period_id = ? AND ebc.new_status = 'IN_PROGRESS'" +
            "   ORDER BY ebc.created DESC LIMIT 1",
            nativeQuery = true)
    Optional<CoverageSearchEvent> findCurrentSearch(int searchId);

    Optional<CoverageSearchEvent> findFirstByCoveragePeriodOrderByCreatedDesc(CoveragePeriod period);
}
