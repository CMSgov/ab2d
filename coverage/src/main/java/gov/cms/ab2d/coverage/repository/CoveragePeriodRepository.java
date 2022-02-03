package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.coverage.model.CoveragePeriod;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoveragePeriodRepository extends JpaRepository<CoveragePeriod, Integer> {

    List<CoveragePeriod> findAllByContractNumber(String contractNumber);

    List<CoveragePeriod> findAllByLastSuccessfulJobIsNull();

    List<CoveragePeriod> findAllByMonthAndYearAndLastSuccessfulJobLessThanEqual(int month, int year, OffsetDateTime time);

    Optional<CoveragePeriod> findByContractNumberAndMonthAndYear(String contractNumber, int month, int year);

    List<CoveragePeriod> findAllByMonthAndYear(int month, int year);
}
