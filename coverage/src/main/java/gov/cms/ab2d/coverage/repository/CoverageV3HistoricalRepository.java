package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.coverage.model.CoverageV3Historical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoverageV3HistoricalRepository extends JpaRepository<CoverageV3Historical, Void> {

    List<CoverageV3Historical> findAllByContract(String contract);

    Optional<CoverageV3Historical> findByContractAndMonthAndYear(String contract, int month, int year);

    List<CoverageV3Historical> findAllByMonthAndYear(int month, int year);

    @Query(value = "SELECT COUNT(c) FROM CoverageV3Historical c WHERE (c.year, c.month) IN (:yearMonthRecords) and c.contract = :contract")
    int getCountByContractAndYearMonthRecords(String contract, List<Integer[]> yearMonthRecords);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM v3.coverage_v3_historical WHERE (year, month) IN (:yearMonthRecords) and contract = :contract")
    int getCountByContractAndYearMonthRecordsNative(@Param("contract") String contract, @Param("yearMonthRecords") List<Integer[]> yearMonthRecords);
}
