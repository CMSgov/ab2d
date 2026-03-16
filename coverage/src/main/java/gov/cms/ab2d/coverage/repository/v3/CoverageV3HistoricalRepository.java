package gov.cms.ab2d.coverage.repository.v3;

import gov.cms.ab2d.coverage.model.v3.CoverageV3Historical;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverageV3HistoricalRepository extends JpaRepository<CoverageV3Historical, Void> {
    List<CoverageV3Historical> findAllByContract(String contract);
    List<CoverageV3Historical> findByContractAndMonthAndYear(String contract, int month, int year);
    List<CoverageV3Historical> findAllByMonthAndYear(int month, int year);
}
