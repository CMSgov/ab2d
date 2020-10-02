package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoverageSearchRepository extends JpaRepository<CoverageSearch, Long> {

    void deleteCoverageSearchByPeriod(CoveragePeriod period);

    Optional<CoverageSearch> findFirstByPeriodOrderByCreatedAsc(CoveragePeriod period);

    Optional<CoverageSearch> findFirstByOrderByCreatedAsc();
}
