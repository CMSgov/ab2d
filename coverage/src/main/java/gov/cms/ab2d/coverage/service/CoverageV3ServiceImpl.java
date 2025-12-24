package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.repository.CoverageV3HistoricalRepository;
import gov.cms.ab2d.coverage.repository.CoverageV3Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CoverageV3ServiceImpl implements CoverageV3Service {

    private final EntityManager em;
    private final CoverageV3Repository coverageV3Repository;
    private final CoverageV3HistoricalRepository coverageV3HistoricalRepository;

    public CoverageV3ServiceImpl(CoverageV3Repository coverageV3Repository, CoverageV3HistoricalRepository coverageV3HistoricalRepository, EntityManager em) {
        this.coverageV3Repository = coverageV3Repository;
        this.coverageV3HistoricalRepository = coverageV3HistoricalRepository;
        this.em = em;
    }

    /**
     select count(*) from v3.coverage_v3_historical
     where (year, month) in
     (
     (2025, 12),
     (2025, 11),
     (2025, 10),
     (2025, 9),
     (2025, 8),
     (2025, 8)
     ) and contract='Z0001'
     */

    public int countBeneficiariesByCoveragePeriod(List<YearMonthRecord> yearMonthRecords, final String contract) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < yearMonthRecords.size(); i++) {
                sb.append("(%s,%s)");
                if (i!=yearMonthRecords.size()-1) {
                    sb.append(",");
                }
            }
            String SQL = "select count(*) from v3.coverage_v3_historical where (year, month) in (" + sb.toString()  +") and contract = :contract";
            log.info("SQL: {}", SQL);
            Query nativeQuery = em.createNativeQuery(SQL);
            nativeQuery.setParameter("contract", contract);
            int count = ((Number) nativeQuery.getSingleResult()).intValue();
            log.info("Count: {}", count);
        }
        catch (Exception e) {
            log.error("oops", e);
        }




        List<Integer[]> yearMonthRecordsAsObjects = new ArrayList<>();
        for (YearMonthRecord yearMonthRecord : yearMonthRecords) {
            Integer[] o = new Integer[]{yearMonthRecord.getYear(), yearMonthRecord.getMonth()};
            yearMonthRecordsAsObjects.add(o);
        }

        try {
            log.info("Count #1: {}", coverageV3Repository.getCountByContractAndYearMonthRecords(contract, yearMonthRecordsAsObjects));
            log.info("Count #2: {}", coverageV3HistoricalRepository.getCountByContractAndYearMonthRecords(contract, yearMonthRecordsAsObjects));
        }
        catch (Exception e) {
            log.error("Error with JPA queries", e);
        }

        try {
            log.info("Count #1: {}", coverageV3Repository.getCountByContractAndYearMonthRecordsNative(contract, yearMonthRecordsAsObjects));
            log.info("Count #2: {}", coverageV3HistoricalRepository.getCountByContractAndYearMonthRecordsNative(contract, yearMonthRecordsAsObjects));
        }
        catch (Exception e) {
            log.error("Error with native queries", e);
        }

        return -1;
    }
    
}
