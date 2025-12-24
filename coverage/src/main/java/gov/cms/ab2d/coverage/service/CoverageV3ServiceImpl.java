package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.repository.CoverageV3HistoricalRepository;
import gov.cms.ab2d.coverage.repository.CoverageV3Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class CoverageV3ServiceImpl implements CoverageV3Service {

    private static final String COVERAGE_V3_TABLE = "v3.coverage_v3";
    private static final String COVERAGE_V3_HISTORICAL_TABLE = "v3.coverage_v3_historical";

    private final EntityManager entityManager;
    private final CoverageV3Repository coverageV3Repository;
    private final CoverageV3HistoricalRepository coverageV3HistoricalRepository;

    public CoverageV3ServiceImpl(
            CoverageV3Repository coverageV3Repository,
            CoverageV3HistoricalRepository coverageV3HistoricalRepository,
            EntityManager entityManager) {
        this.coverageV3Repository = coverageV3Repository;
        this.coverageV3HistoricalRepository = coverageV3HistoricalRepository;
        this.entityManager = entityManager;
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


    static String buildQueryWithPlaceholders(final String table, final int yearMonthRecordsSize) {
        val whereInTupleValues = "(?,?),"
                .repeat(yearMonthRecordsSize-1)
                .concat("(?,?)");

        val sql = String.format("""
            select count(*) from %s
            where 
                contract = ? 
                and (year, month) in (%s) 
        """, table, whereInTupleValues);

        return sql;
    }

    static void populateSqlParameter(Query query, String contract, List<YearMonthRecord> yearMonthRecords) {
        var parameterIndex = 1;
        query.setParameter(parameterIndex++, contract);
        for (YearMonthRecord yearMonthRecord : yearMonthRecords) {
            query.setParameter(parameterIndex++, yearMonthRecord.getYear());
            query.setParameter(parameterIndex++, yearMonthRecord.getMonth());
        }
    }


    public int countBeneficiariesByCoveragePeriod(List<YearMonthRecord> yearMonthRecords, final String contract) {
        val sql1 = buildQueryWithPlaceholders(COVERAGE_V3_TABLE, yearMonthRecords.size());
        val sql2 = buildQueryWithPlaceholders(COVERAGE_V3_HISTORICAL_TABLE, yearMonthRecords.size());

        log.info("sql1: {}", sql1);
        log.info("sql2: {}", sql2);

        Query query1 = entityManager.createNativeQuery(sql1);
        populateSqlParameter(query1, contract, yearMonthRecords);

        log.info("sql1 query string: {}", query1.unwrap(org.hibernate.query.Query.class).getQueryString());

        Query query2 = entityManager.createNativeQuery(sql2);
        populateSqlParameter(query2, contract, yearMonthRecords);
        log.info("sql2 query string: {}", query2.unwrap(org.hibernate.query.Query.class).getQueryString());


        int count1 = ((Number) query1.getSingleResult()).intValue();
        int count2 = ((Number) query2.getSingleResult()).intValue();

        log.info("count1: {}", count1);
        log.info("count2: {}", count2);

        /*
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < yearMonthRecords.size(); i++) {
                sb.append(String.format("(%s,%s)", yearMonthRecords.get(i).getYear(), yearMonthRecords.get(i).getMonth()));
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
        */
        return -1;
    }
    
}
