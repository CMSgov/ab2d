package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageV3;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CoverageV3Repository extends JpaRepository<CoverageV3, Void> {

    List<CoverageV3> findAllByContractNumber(String contractNumber);

    Optional<CoverageV3> findByContractNumberAndMonthAndYear(String contractNumber, int month, int year);

    List<CoverageV3> findAllByMonthAndYear(int month, int year);
}
