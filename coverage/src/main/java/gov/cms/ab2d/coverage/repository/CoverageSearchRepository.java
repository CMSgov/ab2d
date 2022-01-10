package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoverageSearchRepository extends JpaRepository<CoverageSearch, Long> {

    void deleteCoverageSearchByPeriod(CoveragePeriod period);

    Optional<CoverageSearch> findFirstByOrderByCreatedAsc();

    @Query(value = "SELECT cs.* " +
        "   FROM coverage_search cs INNER JOIN bene_coverage_period bcp on cs.bene_coverage_period_id = bcp.id " +
        "   INNER JOIN job j ON bcp.contract_id = j.contract_id " +
        "   WHERE j.status = 'SUBMITTED' " +
        "   ORDER BY j.created_at " +
        "   LIMIT 1", nativeQuery = true)
    Optional<CoverageSearch> findHighestPrioritySearch();
}
