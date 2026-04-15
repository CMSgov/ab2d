package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.service.CoverageV3LockWrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static gov.cms.ab2d.coverage.service.CoverageV3ServiceImpl.executeTimedQuery;
import static java.lang.String.format;

@Slf4j
public class CoverageV3StagingService extends CoverageV3BaseQuery {

    private final CoverageV3LockWrapper lockWrapper;

    public CoverageV3StagingService(DataSource dataSource, CoverageV3LockWrapper lockWrapper) {
        super(dataSource);
        this.lockWrapper = lockWrapper;
    }

//    private static final String COVERAGE_V3_TABLE = "v3.coverage_v3_copy";
//    private static final String COVERAGE_V3_STAGING_TABLE = "v3.coverage_v3_staging_copy";

    // TODO REVERT BACK WHEN DEPLOYING TO DEV

    private static final String COVERAGE_V3_TABLE = "v3.coverage_v3";
    private static final String COVERAGE_V3_STAGING_TABLE = "v3.coverage_v3_staging";

    private static final String RECORD_COUNT_BY_CONTRACT = "select count(*) from %s where contract = :contract";

    private static final String DELETE_RECORDS_FOR_CONTRACT_AND_GET_ROWS_DELETED =
    """
    with deleted_rows as (
        delete from %s where contract = :contract returning *
    )
    select count(*) from deleted_rows
    """;


    private static final String COPY_FROM_STAGING_TO_COVERAGE_V3 =
    """
    with inserted_rows as (
        insert into %s select * from %s where contract = :contract returning *
    )
    select count(*) from inserted_rows;
    """.formatted(COVERAGE_V3_TABLE, COVERAGE_V3_STAGING_TABLE);

    @Transactional
    public boolean copyFromStagingTablesToRecent(String contract) {
        val stagingHelper = new CoverageV3StagingService(dataSource);
        val rowsInStaging = executeTimedQuery(
                format("getCoveragePeriodCountForCoverageV3Staging contract=%s", contract),
                () -> stagingHelper.getCoveragePeriodCountForCoverageV3Staging(contract)
        );
        log.info("Found {} rows in staging table for contract {}", rowsInStaging, contract);
        if (rowsInStaging == 0) {
            return true;
        }

        // TODO acquire lock here

        val rowsInCoverageBeforeCopy = executeTimedQuery(
                format("getCoveragePeriodCountForCoverageV3 contract=%s", contract),
                () -> stagingHelper.getCoveragePeriodCountForCoverageV3(contract)
        );
        log.info("Found {} rows in coverage table for contract {}", rowsInCoverageBeforeCopy, contract);

        log.info("Preparing to delete rows in coverage table for contract {}...", contract);
        val rowsInCoverageDeleted = executeTimedQuery(
                format("deleteFromCoverageAndGetRowsDeleted contract=%s", contract),
                () -> stagingHelper.deleteFromCoverageAndGetRowsDeleted(contract)
        );
        log.info("Deleted {} rows in coverage table for contract {}", rowsInCoverageDeleted, contract);

        log.info("Preparing to copy rows from staging to coverage for contract {}...", contract);
        val rowsInserted = executeTimedQuery(
                format("copyFromStagingToCoverage contract=%s", contract),
                () -> stagingHelper.copyFromStagingToCoverage(contract)
        );
        log.info("Copied {} rows from staging to coverage for contract {}", rowsInserted, contract);

        val rowsInCoverageAfterCopy = executeTimedQuery(
                format("getCoveragePeriodCountForCoverageV3 contract=%s", contract),
                () -> stagingHelper.getCoveragePeriodCountForCoverageV3(contract)
        );
        log.info("Coverage table now contains {} rows for contract {}", rowsInCoverageAfterCopy, contract);

        if (!rowsInStaging.equals(rowsInCoverageAfterCopy)) {
            // TODO throw exception to cause rollback
            log.error("Row count in staging ({}) != row count in coverage ({}) for contract {}",
                    rowsInStaging,
                    rowsInCoverageBeforeCopy,
                    contract
            );
            return false;
        }

        log.info("Preparing to delete rows in staging for contract {}", contract);
        val rowsInStagingDeleted = executeTimedQuery(
                format("deleteFromStagingAndGetRowsDeleted contract=%s", contract),
                () -> stagingHelper.deleteFromStagingAndGetRowsDeleted(contract)
        );

        if (!rowsInStagingDeleted.equals(rowsInCoverageAfterCopy)) {
            // TODO throw exception to cause rollback
            log.error("Row count deleted from staging ({}) != row count in coverage ({}) for contract {}",
                    rowsInStagingDeleted,
                    rowsInCoverageAfterCopy,
                    contract
            );
            return false;
        }

        log.info("Coverage data successfully copied from staging table for contract {}", contract);
        return true;
    }

    @Transactional
    public boolean copyFromStagingTablesToHistorical(String contract) {
        return true;
    }

    public int getCoveragePeriodCountForCoverageV3(final String contract) {
        val formattedQuery = format(RECORD_COUNT_BY_CONTRACT, COVERAGE_V3_TABLE);
        return executeQuery(contract, formattedQuery);
    }

    public int getCoveragePeriodCountForCoverageV3Staging(final String contract) {
        val formattedQuery = format(RECORD_COUNT_BY_CONTRACT, COVERAGE_V3_STAGING_TABLE);
        return executeQuery(contract, formattedQuery);
    }

    public int copyFromStagingToCoverage(final String contract) {
        return executeQuery(contract, COPY_FROM_STAGING_TO_COVERAGE_V3);
    }

    public int deleteFromCoverageAndGetRowsDeleted(String contract) {
        val formattedQuery = format(DELETE_RECORDS_FOR_CONTRACT_AND_GET_ROWS_DELETED, COVERAGE_V3_TABLE);
        return executeQuery(contract, formattedQuery);
    }

    public int deleteFromStagingAndGetRowsDeleted(String contract) {
        val formattedQuery = format(DELETE_RECORDS_FOR_CONTRACT_AND_GET_ROWS_DELETED, COVERAGE_V3_STAGING_TABLE);
        return executeQuery(contract, formattedQuery);
    }

    private int executeQuery(final String contract, final String query) {
        val parameters = new MapSqlParameterSource().addValue("contract", contract);
        val template = new NamedParameterJdbcTemplate(this.dataSource);
        return DataAccessUtils.intResult(template.queryForList(query, parameters, Integer.class));
    }

}
