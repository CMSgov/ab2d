package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageV3;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;

import java.lang.annotation.Native;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CoverageV3Repository extends JpaRepository<CoverageV3, Void> {

    List<CoverageV3> findAllByContract(String contract);

    Optional<CoverageV3> findByContractAndMonthAndYear(String contract, int month, int year);

    List<CoverageV3> findAllByMonthAndYear(int month, int year);

    @Query(value = "SELECT COUNT(c) FROM CoverageV3 c WHERE (c.year, c.month) IN :yearMonthRecords and c.contract = :contract")
    int getCountByContractAndYearMonthRecords(String contract, List<YearMonthRecord> yearMonthRecords);
}
