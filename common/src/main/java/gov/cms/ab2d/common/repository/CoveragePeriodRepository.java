package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoveragePeriodRepository extends JpaRepository<CoveragePeriod, Integer> {

    List<CoveragePeriod> findAllByContractId(Long contractId);

    List<CoveragePeriod> findAllByMonthAndYear(int month, int year);

    List<CoveragePeriod> findAllByStatusIsNull();

    CoveragePeriod getByContractIdAndMonthAndYear(Long contractId, int month, int year);
}
