package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoverageRepository extends JpaRepository<Coverage, Long> {

    int countByCoverageSearchEvent(CoverageSearchEvent searchEvent);

    @Query(value = "SELECT COUNT(*) FROM (" +
                            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = :searchEventId1 " +
                            " INTERSECT " +
                            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = :searchEventId2 " +
                    ") I", nativeQuery = true)
    int countIntersection(long searchEventId1, long searchEventId2);

    @Query(" SELECT c.beneficiaryId " +
            "  FROM Coverage c " +
            " WHERE c.coveragePeriod = :coveragePeriod ")
    List<String> findActiveBeneficiaryIds(CoveragePeriod coveragePeriod);

    int removeAllByCoveragePeriod(CoveragePeriod coveragePeriod);

    int removeAllByCoveragePeriodAndCoverageSearchEvent(CoveragePeriod search, CoverageSearchEvent searchEvent);


}
