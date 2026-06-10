package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static gov.cms.ab2d.common.util.PropertyConstants.*;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3ServiceImpl.executeTimedQuery;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncSource.CRON_JOB;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
import static java.lang.String.format;

@Slf4j
@Component
public class CoverageV3SyncServiceImpl  implements CoverageV3SyncService {

    private final CoverageV3LockWrapper recentCoverageLock;
    private final CoverageV3LockWrapper historicalCoverageLock;
    private final PropertiesService propertiesService;
    private final DataSource dataSource;

    public CoverageV3SyncServiceImpl(
            DataSource dataSource,
            @Qualifier("recentCoverageLock") CoverageV3LockWrapper recentCoverageLock,
            @Qualifier("historicalCoverageLock") CoverageV3LockWrapper historicalCoverageLock,
            PropertiesService propertiesService) {
        this.dataSource = dataSource;
        this.recentCoverageLock = recentCoverageLock;
        this.historicalCoverageLock = historicalCoverageLock;
        this.propertiesService = propertiesService;
    }


    private static final String COVERAGE_V3_TABLE_RECENT = "v3.coverage_v3";
    private static final String COVERAGE_V3_TABLE_HISTORICAL = "v3.coverage_v3_historical";
    private static final String COVERAGE_V3_STAGING_TABLE = "v3.coverage_v3_staging";

    private static final String RECORD_COUNT_BY_CONTRACT =
        "select count(*) from %s where contract = :contract";

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
    """.formatted(COVERAGE_V3_TABLE_RECENT, COVERAGE_V3_STAGING_TABLE);

    private static final String HISTORICAL_SYNC_FOR_CONTRACT =
    """
    with inserted_rows as (
        INSERT INTO %s (patient_id, contract, year, month, current_mbi)
        SELECT coverage.patient_id, coverage.contract, coverage.year, coverage.month, coverage.current_mbi
        FROM %s coverage
        WHERE NOT EXISTS (
            SELECT 1
            FROM %s historical
            WHERE historical.patient_id = coverage.patient_id
              AND historical.contract = coverage.contract
              AND historical.year = coverage.year
              AND historical.month = coverage.month
              AND historical.current_mbi = coverage.current_mbi
        )
        AND contract = :contract
        AND make_date(coverage.year, coverage.month, 1) < (date_trunc('month', CURRENT_DATE) - interval '2 months')::date
        ON CONFLICT (patient_id, contract, year, month, current_mbi)
        DO NOTHING
        RETURNING *
    )
    
    select count(*) from inserted_rows;
    """.formatted(COVERAGE_V3_TABLE_HISTORICAL, COVERAGE_V3_TABLE_RECENT, COVERAGE_V3_TABLE_HISTORICAL);

    private static final String DELETE_OLD_MONTHS_SQL_FOR_CONTRACT =
    """
    with deleted_rows as (
        DELETE FROM %s
        WHERE make_date(year, month, 1) < (date_trunc('month', CURRENT_DATE) - interval '2 months')::date
        and contract = :contract
        RETURNING *
    )
    
    select count(*) from deleted_rows;
    """.formatted(COVERAGE_V3_TABLE_RECENT);

    private static final String DELETE_HISTORY_SUMMARY_FOR_CONTRACT =
        "DELETE FROM v3.coverage_v3_history_summary WHERE contract = :contract";

    private static final String INSERT_HISTORY_SUMMARY_FOR_CONTRACT =
    """
    INSERT INTO v3.coverage_v3_history_summary
        (contract, patient_id, current_mbi, historical_coverage_summaries)
    SELECT contract, patient_id, current_mbi,
           array_agg(array[year, month] ORDER BY year ASC, month ASC)
    FROM v3.coverage_v3_historical
    WHERE contract = :contract
    GROUP BY contract, patient_id, current_mbi
    """;

    private static final String GET_CONTRACTS_WITH_ACTIVE_V3_JOBS =
        "select distinct contract_number from job where status in ('IN_PROGRESS', 'SUBMITTED')";

    private static final String IS_CONTRACT_ATTESTED =
        "select count(*) from contract.contract where contract_number = :contract and attested_on is not null";

    private static final String GET_CONTRACTS_WITH_COVERAGE_IN_STAGING =
        "select distinct contract from %s"
        .formatted(COVERAGE_V3_STAGING_TABLE);

    private static final String GET_CONTRACTS_IN_RECENT_COVERAGE_TABLE =
        "select distinct contract from %s"
        .formatted(COVERAGE_V3_TABLE_RECENT);

    private static final String GET_INACTIVE_CONTRACTS_IN_HISTORY_SUMMARY =
    """
    SELECT DISTINCT contract FROM v3.coverage_v3_history_summary
    WHERE contract IN (
        SELECT contract_number FROM contract.contract
        WHERE attested_on IS NULL
           OR (hpms_end_date IS NOT NULL AND hpms_end_date < :cutoff)
    )
    """;

    private static final String DELETE_INACTIVE_CONTRACTS_FROM_HISTORY_SUMMARY =
    """
    with deleted_rows as (
        DELETE FROM v3.coverage_v3_history_summary
        WHERE contract IN (
            SELECT contract_number FROM contract.contract
            WHERE attested_on IS NULL
               OR (hpms_end_date IS NOT NULL AND hpms_end_date < :cutoff)
        )
        RETURNING contract
    )
    select contract from deleted_rows;
    """;

    @Transactional
    public CoverageV3SyncResult copyFromStagingTablesToRecent(String contract, CoverageV3SyncSource source) {
        if (isTestContract(contract)) {
            return NO_COVERAGE_FOUND_FOR_CONTRACT;
        } else if (!isContractAttested(contract)) {
            log.info("[V3] Contract {} is not attested; skipping staging copy", contract);
            return NO_COVERAGE_FOUND_FOR_CONTRACT;
        } else if (idrImporterInProgress()) {
            return IDR_IMPORTER_IN_PROGRESS;
        } else if (source == CRON_JOB && contractHasJobInProgress(contract)) {
            return JOB_IN_PROGRESS_FOR_CONTRACT;
        }

        val rowsInStaging = executeTimedQuery(
            format("[V3] getCoveragePeriodCountForCoverageV3Staging contract=%s", contract),
            () -> getCoveragePeriodCountForCoverageV3Staging(contract)
        );
        log.info("[V3] Found {} rows in staging table for contract {}", rowsInStaging, contract);
        if (rowsInStaging == 0) {
            return NO_COVERAGE_FOUND_FOR_CONTRACT;
        }

        val lock = recentCoverageLock.getCoverageLock(contract);
        val locked = lock.tryLock();
        try {
            if (locked) {
                return copyFromStagingToRecentCoverageTable(contract, rowsInStaging);
            } else {
                log.info("[V3] Unable to acquire lock for contract {}", contract);
                return UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT;
            }
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private CoverageV3SyncResult copyFromStagingToRecentCoverageTable(final String contract, final int rowsInStaging) {
        val rowsInCoverageBeforeCopy = executeTimedQuery(
                format("[V3] getCoveragePeriodCountForCoverageV3 contract=%s", contract),
                () -> getCoveragePeriodCountForCoverageV3(contract)
        );
        log.info("Found {} rows in coverage table for contract {}", rowsInCoverageBeforeCopy, contract);

        log.info("[V3] Preparing to delete rows in coverage table for contract {}...", contract);
        val rowsInCoverageDeleted = executeTimedQuery(
                format("[V3] deleteFromCoverageAndGetRowsDeleted contract=%s", contract),
                () -> deleteFromCoverageAndGetRowsDeleted(contract)
        );
        log.info("[V3] Deleted {} rows in coverage table for contract {}", rowsInCoverageDeleted, contract);

        log.info("[V3] Preparing to copy rows from staging to coverage for contract {}...", contract);
        val rowsInserted = executeTimedQuery(
                format("[V3] copyFromStagingToCoverage contract=%s", contract),
                () -> copyFromStagingToCoverage(contract)
        );
        log.info("[V3] Copied {} rows from staging to coverage for contract {}", rowsInserted, contract);

        val rowsInCoverageAfterCopy = executeTimedQuery(
                format("[V3] getCoveragePeriodCountForCoverageV3 contract=%s", contract),
                () -> getCoveragePeriodCountForCoverageV3(contract)
        );
        log.info("[V3] Coverage table now contains {} rows for contract {}", rowsInCoverageAfterCopy, contract);

        if (rowsInStaging != rowsInCoverageAfterCopy) {
            log.error("[V3] Row count in staging ({}) != row count in coverage ({}) for contract {}",
                    rowsInStaging,
                    rowsInCoverageBeforeCopy,
                    contract
            );
            return SYNC_FAILED_FOR_CONTRACT;
        }

        log.info("[V3] Preparing to delete rows in staging for contract {}", contract);
        val rowsInStagingDeleted = executeTimedQuery(
                format("[V3] deleteFromStagingAndGetRowsDeleted contract=%s", contract),
                () -> deleteFromStagingAndGetRowsDeleted(contract)
        );

        if (!rowsInStagingDeleted.equals(rowsInCoverageAfterCopy)) {
            log.error("[V3] Row count deleted from staging ({}) != row count in coverage ({}) for contract {}",
                    rowsInStagingDeleted,
                    rowsInCoverageAfterCopy,
                    contract
            );
            return SYNC_FAILED_FOR_CONTRACT;
        }

        log.info("[V3] Coverage data successfully copied from staging table for contract {}", contract);
        return SYNC_SUCCESSFUL_FOR_CONTRACT;
    }

    // Set query timeout of 1 hour, otherwise large contracts may cause org.springframework.dao.QueryTimeoutException
    @Transactional(timeout=3600)
    public CoverageV3SyncResult moveToHistorical(String contract, CoverageV3SyncSource source) {
        if (isTestContract(contract)) {
            return NO_COVERAGE_FOUND_FOR_CONTRACT;
        } else if (source == CRON_JOB && contractHasJobInProgress(contract)) {
            return JOB_IN_PROGRESS_FOR_CONTRACT;
        }

        val lock = historicalCoverageLock.getCoverageLock(contract);
        val locked = lock.tryLock();
        try {
            if (locked) {
                int rowsMoved = moveToHistoricalInternal(contract);
                log.info("[V3] Moved {} rows to historical coverage table for contract {}", rowsMoved, contract);
                if (rowsMoved == 0) {
                    return NO_COVERAGE_FOUND_FOR_CONTRACT;
                } else {
                    populateHistorySummaryForContract(contract);
                }
                int rowsDeleted = deleteMonthsOldCoverage(contract);
                log.info("[V3] Deleted {} rows from recent coverage table for contract {}", rowsDeleted, contract);

                if (rowsDeleted != rowsMoved) {
                    return SYNC_FAILED_FOR_CONTRACT;
                } else {
                    return SYNC_SUCCESSFUL_FOR_CONTRACT;
                }
            } else {
                log.info("[V3] Unable to acquire lock for contract {}", contract);
                return UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT;
            }
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }


    @Override
    public List<String> getContractsInRecentCoverageTable() {
        val template = new JdbcTemplate(this.dataSource);
        return template.queryForList(GET_CONTRACTS_IN_RECENT_COVERAGE_TABLE, String.class);
    }

    @Override
    public List<String> getContractsInCoverageStagingTable() {
        val template = new JdbcTemplate(this.dataSource);
        return template.queryForList(GET_CONTRACTS_WITH_COVERAGE_IN_STAGING, String.class);
    }

    @Override
    public List<String> getContractsWithActiveV3Jobs() {
        val template = new JdbcTemplate(this.dataSource);
        return template.queryForList(GET_CONTRACTS_WITH_ACTIVE_V3_JOBS, String.class);
    }

   int getCoveragePeriodCountForCoverageV3(final String contract) {
        val formattedQuery = format(RECORD_COUNT_BY_CONTRACT, COVERAGE_V3_TABLE_RECENT);
        return executeQueryForContract(contract, formattedQuery);
    }

   int getCoveragePeriodCountForCoverageV3Staging(final String contract) {
        val formattedQuery = format(RECORD_COUNT_BY_CONTRACT, COVERAGE_V3_STAGING_TABLE);
        return executeQueryForContract(contract, formattedQuery);
    }

    int copyFromStagingToCoverage(final String contract) {
        return executeQueryForContract(contract, COPY_FROM_STAGING_TO_COVERAGE_V3);
    }

    int moveToHistoricalInternal(final String contract) {
        val formattedQuery = format(HISTORICAL_SYNC_FOR_CONTRACT, contract);
        return executeQueryForContract(contract, formattedQuery);
    }

    int deleteMonthsOldCoverage(final String contract) {
        val formattedQuery = format(DELETE_OLD_MONTHS_SQL_FOR_CONTRACT, contract);
        return executeQueryForContract(contract, formattedQuery);
    }

    int deleteFromCoverageAndGetRowsDeleted(String contract) {
        val formattedQuery = format(DELETE_RECORDS_FOR_CONTRACT_AND_GET_ROWS_DELETED, COVERAGE_V3_TABLE_RECENT);
        return executeQueryForContract(contract, formattedQuery);
    }

    int deleteFromStagingAndGetRowsDeleted(String contract) {
        val formattedQuery = format(DELETE_RECORDS_FOR_CONTRACT_AND_GET_ROWS_DELETED, COVERAGE_V3_STAGING_TABLE);
        return executeQueryForContract(contract, formattedQuery);
    }

    int executeQueryForContract(final String contract, final String query) {
        val parameters = new MapSqlParameterSource().addValue("contract", contract);
        val template = new NamedParameterJdbcTemplate(this.dataSource);
        return DataAccessUtils.intResult(template.queryForList(query, parameters, Integer.class));
    }

    private void populateHistorySummaryForContract(String contract) {
        val parameters = new MapSqlParameterSource().addValue("contract", contract);
        val template = new NamedParameterJdbcTemplate(dataSource);
        template.update(DELETE_HISTORY_SUMMARY_FOR_CONTRACT, parameters);
        int rowsInserted = template.update(INSERT_HISTORY_SUMMARY_FOR_CONTRACT, parameters);
        log.info("[V3] Inserted {} rows into coverage_v3_history_summary for contract {}", rowsInserted, contract);
    }

    boolean isTestContract(String contract) {
        return contract.toUpperCase(Locale.ROOT).startsWith("Z");
    }

    boolean contractHasJobInProgress(String contract) {
        return getContractsWithActiveV3Jobs().contains(contract);
    }

    boolean isContractAttested(String contract) {
        val parameters = new MapSqlParameterSource().addValue("contract", contract);
        val template = new NamedParameterJdbcTemplate(this.dataSource);
        Integer count = DataAccessUtils.intResult(template.queryForList(IS_CONTRACT_ATTESTED, parameters, Integer.class));
        return count != null && count > 0;
    }

    @Override
    public boolean idrImporterInProgress() {
        val idrImporterStatus = propertiesService.getProperty(V3_IDR_IMPORTER_STATUS, "");
        return V3_IDR_IMPORTER_STATUS_IN_PROGRESS.equals(idrImporterStatus);
    }

    @Override
    @Transactional
    public int deleteInactiveContractsFromHistorySummary() {
        LocalDate cutoff = LocalDate.now().minusYears(2);
        val parameters = new MapSqlParameterSource().addValue("cutoff", cutoff);
        val template = new NamedParameterJdbcTemplate(this.dataSource);

        List<String> inactiveContracts = template.queryForList(GET_INACTIVE_CONTRACTS_IN_HISTORY_SUMMARY, parameters, String.class);
        log.info("[V3] Inactive contracts to be purged from history summary ({}): {}", inactiveContracts.size(), inactiveContracts);

        List<String> deletedContracts = template.queryForList(DELETE_INACTIVE_CONTRACTS_FROM_HISTORY_SUMMARY, parameters, String.class);
        log.info("[V3] Deleted {} rows from coverage_v3_history_summary for unattested contracts or contracts ended > 2 years ago", deletedContracts.size());
        return deletedContracts.size();
    }


}
