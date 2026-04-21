package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static gov.cms.ab2d.common.util.PropertyConstants.IDR_IMPORTER_STATUS_IN_PROGRESS;
import static gov.cms.ab2d.common.util.PropertyConstants.IDR_IMPORTER_STATUS_PROPERTY;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3ServiceImpl.executeTimedQuery;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncSource.CRON_JOB;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
import static java.lang.String.format;

@Slf4j
@Component
public class CoverageV3SyncServiceImpl  implements CoverageV3SyncService {

    private final CoverageV3LockWrapper lockWrapper;
    private final PropertiesService propertiesService;
    private final DataSource dataSource;

    public CoverageV3SyncServiceImpl(
            DataSource dataSource,
            CoverageV3LockWrapper lockWrapper,
            PropertiesService propertiesService) {
        this.dataSource = dataSource;
        this.lockWrapper = lockWrapper;
        this.propertiesService = propertiesService;
    }

    // TODO REMOVE - TEMPORARY ONLY FOR DEPLOYING TO DEV
    private static final String DEV_MODIFIER = "_copy";

    private static final String COVERAGE_V3_TABLE_RECENT = "v3.coverage_v3"                 + DEV_MODIFIER;
    private static final String COVERAGE_V3_TABLE_HISTORICAL = "v3.coverage_v3_historical"  + DEV_MODIFIER;
    private static final String COVERAGE_V3_STAGING_TABLE = "v3.coverage_v3_staging"        + DEV_MODIFIER;

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

    private static final String GET_CONTRACTS_WITH_ACTIVE_V3_JOBS =
        "select distinct contract_number from job where status in ('IN_PROGRESS', 'SUBMITTED')";

    private static final String GET_CONTRACTS_WITH_COVERAGE_IN_STAGING =
        "select distinct contract from %s"
        .formatted(COVERAGE_V3_STAGING_TABLE);

    private static final String GET_CONTRACTS_IN_RECENT_COVERAGE_TABLE =
        "select distinct contract from %s"
        .formatted(COVERAGE_V3_TABLE_RECENT);


    // Return true if nothing in staging OR copy is successful
    // Return false if lock can't be acquired or job is running for contract
    // Throw exception is data integrity assertions fail
    @Transactional
    public CoverageV3SyncResult copyFromStagingTablesToRecent(String contract, CoverageV3SyncSource source) {
        if (isTestContract(contract)) {
            return NO_COVERAGE_FOUND_FOR_CONTRACT;
        } else if (idrExporterInProgress()) {
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

        val lock = lockWrapper.getCoverageLock(contract);
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

    @Transactional
    public CoverageV3SyncResult moveToHistorical(String contract, CoverageV3SyncSource source) {
        if (isTestContract(contract)) {
            return NO_COVERAGE_FOUND_FOR_CONTRACT;
        } else if (source == CRON_JOB && contractHasJobInProgress(contract)) {
            return JOB_IN_PROGRESS_FOR_CONTRACT;
        }

        val lock = lockWrapper.getCoverageLock(contract);
        val locked = lock.tryLock();
        try {
            if (locked) {
                int rowsMoved = moveToHistoricalInternal(contract);
                log.info("[V3] Moved {} rows to historical coverage table for contract {}", rowsMoved, contract);
                if (rowsMoved == 0) {
                    return NO_COVERAGE_FOUND_FOR_CONTRACT;
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

    public List<String> getContractsInRecentCoverageTable() {
        val template = new JdbcTemplate(this.dataSource);
        return template.queryForList(GET_CONTRACTS_IN_RECENT_COVERAGE_TABLE, String.class);
    }

    public List<String> getContractsInCoverageStagingTable() {
        val template = new JdbcTemplate(this.dataSource);
        return template.queryForList(GET_CONTRACTS_WITH_COVERAGE_IN_STAGING, String.class);
    }

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

    boolean isTestContract(String contract) {
        return contract.toUpperCase(Locale.ROOT).startsWith("Z");
    }

    boolean contractHasJobInProgress(String contract) {
        return getContractsWithActiveV3Jobs().contains(contract);
    }

    boolean idrExporterInProgress() {
        val idrImporterStatus = propertiesService.getProperty(IDR_IMPORTER_STATUS_PROPERTY, "");
        return IDR_IMPORTER_STATUS_IN_PROGRESS.equals(idrImporterStatus);
    }

}
