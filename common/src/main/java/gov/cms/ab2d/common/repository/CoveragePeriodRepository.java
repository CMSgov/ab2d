package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoveragePeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CoveragePeriodRepository extends JpaRepository<CoveragePeriod, Integer> {

    List<CoveragePeriod> findAllByContractId(Long contractId);

    List<CoveragePeriod> findAllByMonthAndYear(int month, int year);

    List<CoveragePeriod> findAllByLastSuccessfulJobIsNull();

    List<CoveragePeriod> findAllByMonthAndYearAndLastSuccessfulJobLessThanEqual(int month, int year, OffsetDateTime time);

    Optional<CoveragePeriod> findByContractIdAndMonthAndYear(long contractId, int month, int year);

    CoveragePeriod getByContractIdAndMonthAndYear(Long contractId, int month, int year);
}
