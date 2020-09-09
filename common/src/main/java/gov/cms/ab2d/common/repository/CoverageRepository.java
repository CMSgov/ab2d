package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.CoverageSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoverageRepository extends JpaRepository<Coverage, Long> {

    @Query(" SELECT c.beneficiaryId " +
            "  FROM Coverage c " +
            " WHERE c.coverageSearch = :coverageSearch ")
    List<String> findActiveBeneficiaryIds(CoverageSearch coverageSearch);

    @Modifying
    @Query(" DELETE FROM Coverage c  " +
            " WHERE c.coverageSearch IN :coverageSearches")
    int deleteByCoverageId(List<CoverageSearch> coverageSearches);

    @Modifying
    @Query(" DELETE FROM Coverage c " +
            " WHERE c.coverageSearch = :coverageSearch  ")
    int deleteByCoverageSearchId(CoverageSearch coverageSearch);
}
