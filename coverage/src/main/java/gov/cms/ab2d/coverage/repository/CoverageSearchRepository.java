package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverageSearchRepository extends JpaRepository<CoverageSearch, Long> {

    void deleteCoverageSearchByPeriod(CoveragePeriod period);

    Optional<CoverageSearch> findFirstByOrderByCreatedAsc();

    @Query(value = "SELECT cs.* " +
        "   FROM coverage_search cs INNER JOIN bene_coverage_period bcp on cs.bene_coverage_period_id = bcp.id " +
        "   INNER JOIN contract c ON bcp.contract_number = c.contract_number" +
        "   INNER JOIN job j ON c.contract_number = j.contract_number " +
        "   WHERE j.status = 'SUBMITTED' " +
        "   ORDER BY j.created_at " +
        "   LIMIT 1", nativeQuery = true)
    Optional<CoverageSearch> findHighestPrioritySearch();
}
