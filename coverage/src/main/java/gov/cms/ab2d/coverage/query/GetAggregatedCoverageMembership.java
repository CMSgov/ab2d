package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.filter.FilterOutByDate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

@Slf4j
public class GetAggregatedCoverageMembership extends CoverageV3BaseQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GetAggregatedCoverageMembership(DataSource dataSource) {
        super(dataSource);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public static final String AGGREGATED_TABLE_NAME = "v3.coverage_v3_aggregated_{0}";
    private static final String CREATE_AGGREGATED_TABLE =
    """
    DROP TABLE IF EXISTS coverage_v3_temp_{0};
    
    DROP TABLE IF EXISTS v3.coverage_v3_aggregated_{0};
    
    CREATE TEMP TABLE IF NOT EXISTS coverage_v3_temp_{0} AS (
      SELECT
        contract,
        patient_id,
        current_mbi,
        array_agg(ARRAY[year, month]) AS recent_coverage_summaries
      FROM
      (
         SELECT * FROM v3.coverage_v3
         WHERE contract = ''{0}''
      )
      GROUP BY
        contract,
        patient_id,
        current_mbi
    );
    
    CREATE TABLE v3.coverage_v3_aggregated_{0} AS
    SELECT
      recent.contract AS contract_recent,
      history.contract AS contract_history,
      patient_id,
      current_mbi,
      historical_coverage_summaries,
      recent_coverage_summaries,
      share_data,
      ROW_NUMBER() OVER (ORDER BY patient_id) AS row_number
    FROM
      coverage_v3_temp_{0} AS recent
      FULL OUTER JOIN
      (
        SELECT *
        FROM v3.coverage_v3_history_summary
        WHERE contract = ''{0}''
      ) AS history
    USING (patient_id, current_mbi) AS aggregated_results
    LEFT JOIN current_mbi ON aggregated_results.current_mbi = current_mbi.mbi;
    
    CREATE INDEX ON v3.coverage_v3_aggregated_{0} (patient_id);
    
    DROP TABLE coverage_v3_temp_{0};
    """;


    // NEW
    private static final String AGGREGATED_TABLE_ROW_COUNT =
            "SELECT COUNT(DISTINCT patient_id) FROM v3.coverage_v3_aggregated_{0}";

    private static final String GET_DISTINCT_COVERAGE_PERIOD_COUNT =
    """
    SELECT COUNT(*) FROM
    (
        SELECT DISTINCT json_array_elements(to_json(recent_coverage_summaries))::TEXT
        FROM v3.coverage_v3_aggregated_{0}
        UNION
        SELECT DISTINCT json_array_elements(to_json(historical_coverage_summaries))::TEXT
        from v3.coverage_v3_aggregated_{0}
    )
    """;


    private static final String FETCH_FROM_AGGREGATED_TABLE_WITHOUT_CURSOR =
    """
    SELECT patient_id,
       current_mbi,
       contract_history,
       contract_recent,
       historical_coverage_summaries,
       recent_coverage_summaries,
       share_data,
       row_number
    FROM v3.coverage_v3_aggregated_{0}
    ORDER BY patient_id asc
    FETCH FIRST :limit ROWS WITH TIES
    """;

    private static final String FETCH_FROM_AGGREGATED_TABLE_WITH_CURSOR =
    """
    SELECT patient_id,
       current_mbi,
       contract_history,
       contract_recent,
       historical_coverage_summaries,
       recent_coverage_summaries,
       share_data,
       row_number
    FROM v3.coverage_v3_aggregated_{0}
        WHERE patient_id > :patient_id_cursor
    ORDER BY patient_id asc
    FETCH FIRST :limit ROWS WITH TIES
    """;

    public void createAggregatedAttributionTable(final String contract) {
        val tableName = MessageFormat.format(AGGREGATED_TABLE_NAME, contract);
        log.info("Attempting to create aggregated attribution table {}", tableName);
        try {
            val query = MessageFormat.format(CREATE_AGGREGATED_TABLE, contract);
            jdbcTemplate.getJdbcOperations().execute(query);
        } catch (Exception e) {
            log.info("Error creating {}", tableName);
            throw new RuntimeException("Error creating " + tableName);
        }
        log.info("Created table {}", tableName);
    }

    public int getAggregatedTableRowCount(final String contract) {
        val tableName = MessageFormat.format(AGGREGATED_TABLE_NAME, contract);
        log.info("Calculating row count for {}", tableName);
        val query = MessageFormat.format(AGGREGATED_TABLE_ROW_COUNT, contract);
        val rowCount = DataAccessUtils.intResult(jdbcTemplate.getJdbcOperations().queryForList(query, Integer.class));
        log.info("Row count for {} = {}", tableName, rowCount);
        return rowCount;
    }

    public void deleteAggregatedTable(final String contract) {
        val tableName = MessageFormat.format(AGGREGATED_TABLE_NAME, contract);
        log.info("Preparing to delete table {}", tableName);
        val query = MessageFormat.format("DROP TABLE IF EXISTS {0}", tableName);
        jdbcTemplate.getJdbcOperations().execute(query);
        log.info("Deleted table {}", tableName);
    }

    public List<CoverageSummary> fetchAggregatedData(
            final ContractForCoverageDTO contractDto,
            final long limit,
            final Optional<Long> cursor) {

        val parameters = new MapSqlParameterSource()
                .addValue("limit", limit);

        final String query;
        if (cursor.isPresent()) {
            parameters.addValue("patient_id_cursor", cursor.get());
            query = MessageFormat.format(FETCH_FROM_AGGREGATED_TABLE_WITH_CURSOR, contractDto.getContractNumber());
        } else {
            query = MessageFormat.format(FETCH_FROM_AGGREGATED_TABLE_WITHOUT_CURSOR, contractDto.getContractNumber());
        }

        return jdbcTemplate.query(query, parameters, new AggregatedDataRowMapper(contractDto));
    }

    // Returns last patient ID
    public long reduceAndFilter(List<CoverageSummary> summaries) {
        if (summaries.isEmpty()) {
            return -1L;
        }
        val lastPatientId = summaries.get(summaries.size()-1).getIdentifiers().getPatientIdV3();

        // Find indexes of subsequent record where patient ID is the same
        // E.g. [ [0,1], [5,6,7] ] means records 1 and 2 have the same patient ID (differnet MBI)
        // and records 6, 7, and 8 have the same patient ID.
        List<List<Integer>> indexesOfDuplicatePatients = getIndexesOfDuplicatePatients(summaries);

        // For each set of duplicate patient ID records, reduce (combined to one record)
        reduce(indexesOfDuplicatePatients, summaries);

        // remove any null values (as a result of reducing duplicate patient records)
        // remove any records where a patient has opted out
        val iterator = summaries.listIterator();
        while (iterator.hasNext()) {
            val next = iterator.next();
            if (next == null) {
                iterator.remove();
            } else {
                val identifiers = next.getIdentifiers();
                if (Objects.equals(Boolean.FALSE, identifiers.getShareDataV3())) {
                    log.info("Patient at row number {} has opted out; removing coverage summary", identifiers.getRowNumberV3());
                    iterator.remove();
                }
            }
        }

        return lastPatientId;
    }

    List<List<Integer>> getIndexesOfDuplicatePatients(List<CoverageSummary> summaries) {
        if (summaries.size() < 2) {
            return Collections.emptyList();
        }

        val result = new LinkedList<List<Integer>>();

        LinkedHashSet<Integer> intermediateResult = null;
        var index = 0;
        var current = summaries.get(index);
        while (index < summaries.size() - 1) {

            val next = summaries.get(index+1);
            if (current.getIdentifiers().getPatientIdV3() == next.getIdentifiers().getPatientIdV3()) {
                if (intermediateResult == null) {
                    intermediateResult = new LinkedHashSet<>();
                }
                intermediateResult.add(index);
                intermediateResult.add(index+1);
            } else {
                if (intermediateResult != null) {
                    result.add(new ArrayList<>(intermediateResult));
                    intermediateResult = null;
                }
            }

            current = next;
            index++;
        }

        if (intermediateResult != null) {
            result.add(new ArrayList<>(intermediateResult));
        }

        return result;
    }

    CoverageSummary reduceSummariesForDuplicatePatientId(List<CoverageSummary> summaries) {
        val patientId = summaries.get(0).getIdentifiers().getPatientIdV3(); // TODO remove assertion

        var shareData = true;
        val dateRanges = new ArrayList<FilterOutByDate.DateRange>();

        for (CoverageSummary summary : summaries) {
            val identifiers = summary.getIdentifiers();

            if (identifiers.getPatientIdV3() != patientId) {
                throw new IllegalStateException(); // TODO remove assertion
            }

            if (identifiers.getShareDataV3() != null && identifiers.getShareDataV3() == false) {
                shareData = false;
            }

            if (summary.getDateRanges() != null) {
                dateRanges.addAll(summary.getDateRanges());
            }
        }

        val firstSummary = summaries.get(0);
        val firstSummaryIdentifier = firstSummary.getIdentifiers();
        val newIdentifier = Identifiers.ofV3(
            firstSummaryIdentifier.getPatientIdV3(),
            firstSummaryIdentifier.getCurrentMbi(),
            shareData,
            firstSummaryIdentifier.getRowNumberV3()
        );

        return new CoverageSummary(newIdentifier, firstSummary.getContract(), dateRanges);

    }

    void reduce(List<List<Integer>> indexList, List<CoverageSummary> summaries) {
        for (List<Integer> indexSubList : indexList) {
            val summarySubList = new ArrayList<CoverageSummary>(indexSubList.size());
            for (int index : indexSubList) {
                val summary = summaries.get(index);
                summarySubList.add(summary);
                // set element to null instead of removing in order to prevent shifting indexes
                // nulls will be removed later
                summaries.set(index, null);
                log.info("Processing duplicate patient at row number {}", summary.getIdentifiers().getRowNumberV3());
            }

            val reducedSummary = reduceSummariesForDuplicatePatientId(summarySubList);
            val firstIndex = indexSubList.get(0);
            summaries.set(firstIndex, reducedSummary);
            log.info("Reduced duplicate patient records into record from row number {}", reducedSummary.getIdentifiers().getRowNumberV3());
        }
    }

    public int getCoveragePeriodsInAggregatedTable(String contract) {
        val query = MessageFormat.format(GET_DISTINCT_COVERAGE_PERIOD_COUNT, contract);
        return DataAccessUtils.intResult(jdbcTemplate.getJdbcOperations().queryForList(query, Integer.class));
    }

    private static class AggregatedDataRowMapper implements RowMapper<CoverageSummary> {

        private final ContractForCoverageDTO contractDto;

        public AggregatedDataRowMapper(ContractForCoverageDTO contractDto) {
            this.contractDto = contractDto;
        }

        @Override
        public CoverageSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            val shareData = rs.getObject(7, Boolean.class);
            val rowNumber = rs.getLong(8);

            val patientId = rs.getLong(1);
            val currentMbi = rs.getString(2);
            val identifiers = Identifiers.ofV3(patientId, currentMbi, shareData, rowNumber);

            // These are not used -- useful for determining if record came from historical or recent coverage table
            val contractHistory = rs.getString(3);
            val contractRecent = rs.getString(4);

            val historicalSummary = rs.getArray(5);
            Integer[][] historicalSummaryArray = historicalSummary == null
                    ? null
                    : (Integer[][]) historicalSummary.getArray();

            val recentSummary = rs.getArray(6);
            Integer[][] recentSummaryArray = recentSummary == null
                    ? null
                    : (Integer[][]) recentSummary.getArray();

            val coverageMembershipList = toCoverageMembershipList(identifiers, historicalSummaryArray, recentSummaryArray);
            return CoverageServiceRepository.summarizeCoverageMembership(contractDto, coverageMembershipList);

        }

        private List<CoverageMembership> toCoverageMembershipList(
                final Identifiers identifiers,
                final Integer[][] historicalList,
                final Integer[][] recentList) {

            int listSize = size(historicalList) + size(recentList);
            val result = new ArrayList<CoverageMembership>(listSize);

            if (historicalList != null) {
                for (Integer[] historicalCoverageItem : historicalList) {
                    val coverageMembership = toCoverageMembership(identifiers, historicalCoverageItem);
                    result.add(coverageMembership);
                }
            }

            if (recentList != null) {
                for (Integer[] recentCoverageItem : recentList) {
                    val coverageMembership = toCoverageMembership(identifiers, recentCoverageItem);
                    result.add(coverageMembership);
                }
            }

            return result;

        }

        private CoverageMembership toCoverageMembership(Identifiers identifiers, Integer[] array) {
            val year = array[0];
            val month = array[1];
            return new CoverageMembership(identifiers, year, month);
        }

        private int size(Integer[][] array) {
            return array == null ? 0 : array.length;
        }

    }



}
