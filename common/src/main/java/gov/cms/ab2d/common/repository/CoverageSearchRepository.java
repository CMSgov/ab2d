package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoverageSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverageSearchRepository extends JpaRepository<CoverageSearch, Integer> {

    List<CoverageSearch> findAllByContractId(Long contractId);

    List<CoverageSearch> findAllByMonthAndYear(int month, int year);

    CoverageSearch getByContractIdAndMonthAndYear(Long contractId, int month, int year);
}
