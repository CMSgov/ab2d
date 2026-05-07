package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GetAggregatedCoverageMembership extends CoverageV3BaseQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GetAggregatedCoverageMembership(DataSource dataSource) {
        super(dataSource);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    private static final String AGGREGATED_TABLE_NAME = "v3.coverage_v3_aggregated_{0}";
    private static final String CREATE_AGGREGATED_ATTRIBUTION_DATA =
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

    public static void main(String[] args) {
        System.out.println(MessageFormat.format(CREATE_AGGREGATED_ATTRIBUTION_DATA, "S9701"));
    }

    private static final String AGGREGATED_TABLE_ROW_COUNT = "select count(*) from v3.coverage_v3_aggregated_{0};";

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


    /** TODO

     update with

     SELECT patient_id,
     current_mbi,
     contract_history,
     contract_recent,
     historical_coverage_summaries,
     recent_coverage_summaries
     FROM v3.coverage_v3_aggregated_s9701
     where patient_id > 143851408
     ORDER BY patient_id asc
     FETCH FIRST 1000 ROWS WITH TIES;


     */

    private static final String FETCH_AGGREGATED_DATA_WITHOUT_CURSOR =
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
    FETCH FIRST :limit ROWS WITH TIES;
    """;

    private static final String FETCH_AGGREGATED_DATA_WITH_CURSOR =
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
    FETCH FIRST :limit ROWS WITH TIES;
    """;

    public void createAggregatedAttributionTable(final String contract) {
        val tableName = MessageFormat.format(AGGREGATED_TABLE_NAME, contract);
        log.info("Attempting to create aggregated attribution table {}", tableName);
        try {
            val query = MessageFormat.format(CREATE_AGGREGATED_ATTRIBUTION_DATA, contract);
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

    public List<CoverageSummary> fetchAggregatedData(
            final ContractForCoverageDTO contractDto,
            final long limit,
            final Optional<Long> cursor) {

        val parameters = new MapSqlParameterSource()
                .addValue("limit", limit);

        final String query;
        if (cursor.isPresent()) {
            parameters.addValue("patient_id_cursor", cursor.get());
            query = MessageFormat.format(FETCH_AGGREGATED_DATA_WITH_CURSOR, contractDto.getContractNumber());
        } else {
            query = MessageFormat.format(FETCH_AGGREGATED_DATA_WITHOUT_CURSOR, contractDto.getContractNumber());
        }

        List<CoverageSummary> coverageSummaries = jdbcTemplate.query(query, parameters, new AggregatedDataRowMapper(contractDto));
        // TODO: Need to filter to find any grouping of patient ID, combine dates, and determine if patient has opted-out
        return coverageSummaries;
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

            if (shareData == null || shareData) {
                val coverageMembershipList = toCoverageMembershipList(identifiers, historicalSummaryArray, recentSummaryArray);
                return CoverageServiceRepository.summarizeCoverageMembership(contractDto, coverageMembershipList);

            } else {
                return new CoverageSummary(identifiers, contractDto, Collections.emptyList());
            }

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
