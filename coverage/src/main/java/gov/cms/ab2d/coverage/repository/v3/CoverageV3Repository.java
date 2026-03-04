package gov.cms.ab2d.coverage.repository.v3;

import gov.cms.ab2d.coverage.model.v3.CoverageV3;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverageV3Repository extends JpaRepository<CoverageV3, Void> {
    List<CoverageV3> findAllByContract(String contract);
    List<CoverageV3> findByContractAndMonthAndYear(String contract, int month, int year);
    List<CoverageV3> findAllByMonthAndYear(int month, int year);
}
