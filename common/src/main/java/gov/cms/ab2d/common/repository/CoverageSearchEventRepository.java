package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CoverageSearchEventRepository extends JpaRepository<CoverageSearchEvent, Long> {

    @Query(value = "SELECT * FROM event_bene_coverage_search_status_change as ebc " +
            "   WHERE ebc.bene_coverage_search_id = ? AND ebc.new_status = 'IN_PROGRESS'" +
            "   ORDER BY ebc.occurredAt DESC LIMIT 1 OFFSET ?", nativeQuery = true)
    Optional<CoverageSearchEvent> findSearch(int searchId, int searchNumber);

}
