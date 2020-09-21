package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = " SELECT DISTINCT c.beneficiaryId " +
            "  FROM Coverage c " +
            " WHERE c.coveragePeriod IN :coveragePeriods " +
            " ORDER BY c.beneficiaryId")
    List<String> findActiveBeneficiaryIds(List<CoveragePeriod> coveragePeriods);

    @Query(value = " SELECT DISTINCT c.beneficiaryId " +
            "  FROM Coverage c " +
            " WHERE c.coveragePeriod IN :coveragePeriods " +
            " ORDER BY c.beneficiaryId",
            countQuery = " SELECT DISTINCT COUNT(c.beneficiaryId) " +
                    "  FROM Coverage c " +
                    " WHERE c.coveragePeriod IN :coveragePeriods ")
    Page<String> findActiveBeneficiaryIds(List<CoveragePeriod> coveragePeriods, Pageable pageable);

    @Query(value = "SELECT cov.beneficiary_id, period.year, period.month " +
            "FROM bene_coverage_period AS period INNER JOIN " +
            " (SELECT * FROM coverage AS cov WHERE cov.bene_coverage_period_id IN (:coveragePeriods) AND cov.beneficiary_id IN (:beneficiaryIds)) AS cov " +
            " ON cov.bene_coverage_period_id = period.id " +
            "ORDER BY period.year, period.month ASC", nativeQuery = true)
    List<Object[]> findCoverageInformation(List<Integer> coveragePeriods, List<String> beneficiaryIds);


    int removeAllByCoveragePeriod(CoveragePeriod coveragePeriod);

    int removeAllByCoveragePeriodAndCoverageSearchEvent(CoveragePeriod search, CoverageSearchEvent searchEvent);


}
