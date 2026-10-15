package gov.cms.ab2d.importer;

import datadog.trace.api.Trace;
import gov.cms.ab2d.common.util.DatadogSpans;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Writes one backfilled month for one contract into v3.coverage_v3_historical and rebuilds that
 * contract's summary tables, as a single transaction.
 */
@Component
@Slf4j
public class CoverageV3HistoricalBackfill {

    static final String LOAD_TABLE = "v3.coverage_v3_backfill";
    static final String LOCK_NAME_PREFIX = "COVERAGE_V3_HISTORICAL_LOCK_";
    static final Duration LOCK_WAIT = Duration.ofMinutes(30);
    static final Duration TRANSACTION_TIMEOUT = Duration.ofHours(3);
    static final Duration LOCK_TTL = TRANSACTION_TIMEOUT.plusHours(1);
    private static final String HISTORY_SUMMARY_WORK_MEM = "512MB";

    static final String GET_CONTRACTS_WITH_ACTIVE_V3_JOBS =
            "SELECT DISTINCT contract_number FROM job WHERE status IN ('IN_PROGRESS', 'SUBMITTED') AND fhir_version='R4V3'";

    static final String QUERY_BFD_COVERAGE_SYNC_IN_PROGRESS =
            "SELECT count(*) FROM ab2d.bene_coverage_period WHERE status='IN_PROGRESS'";

    private static final String COUNT_EXTRACT_ROWS =
            "SELECT count(*) FROM " + LOAD_TABLE + " WHERE contract = ? AND year = ? AND month = ?";

    private static final String INSERT_HISTORICAL = """
            INSERT INTO v3.coverage_v3_historical (patient_id, contract, year, month, current_mbi)
            SELECT DISTINCT b.patient_id, b.contract, b.year, b.month, b.current_mbi
            FROM v3.coverage_v3_backfill b
            WHERE b.contract = ? AND b.year = ? AND b.month = ?
              AND NOT EXISTS (
                  SELECT 1 FROM v3.coverage_v3_historical h
                  WHERE h.patient_id = b.patient_id
                    AND h.contract = b.contract
                    AND h.year = b.year
                    AND h.month = b.month
                    AND h.current_mbi IS NOT DISTINCT FROM b.current_mbi
              )
            ON CONFLICT (patient_id, contract, year, month, current_mbi) DO NOTHING
            """;

    static final String COUNT_COVERAGE_PERIOD = """
            SELECT count(*) FROM v3.coverage_v3_history_summary_coverage_periods
            WHERE contract = ? AND year = ? AND month = ?
            """;

    private static final String DELETE_HISTORY_SUMMARY =
            "DELETE FROM v3.coverage_v3_history_summary WHERE contract = ?";

    private static final String INSERT_HISTORY_SUMMARY = """
            INSERT INTO v3.coverage_v3_history_summary
                (contract, patient_id, current_mbi, historical_coverage_summaries)
            SELECT contract, patient_id, current_mbi,
                   array_agg(array[year, month] ORDER BY year ASC, month ASC)
            FROM v3.coverage_v3_historical
            WHERE contract = ?
            GROUP BY contract, patient_id, current_mbi
            """;

    private static final String INSERT_COVERAGE_PERIODS = """
            INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
            SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = ?
            ON CONFLICT (contract, year, month) DO NOTHING
            """;

    private static final String COUNT_HISTORICAL_ROWS =
            "SELECT count(*) FROM v3.coverage_v3_historical WHERE contract = ? AND year = ? AND month = ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final JdbcLockRegistry lockRegistry;

    public CoverageV3HistoricalBackfill(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            JdbcLockRegistry historicalCoverageLockRegistry
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout((int) TRANSACTION_TIMEOUT.toSeconds());
        this.lockRegistry = historicalCoverageLockRegistry;
    }

    /**
     * Throws if the contract was skipped or failed; the caller reports it and moves on to the next contract.
     */
    @Trace(operationName = "ab2d.idr.backfill_contract")
    public void backfill(String contract, YearMonth month) throws InterruptedException {
        DatadogSpans.setTag("component", "idr");
        DatadogSpans.setTag("contract", contract);
        DatadogSpans.setTag("backfill.month", month.toString());

        // The same checks the worker's moveToHistorical makes before it archives a contract.
        if (isTestContract(contract)) {
            throw new IllegalStateException("Contract " + contract + " is a test contract and is never archived");
        }
        if (count(QUERY_BFD_COVERAGE_SYNC_IN_PROGRESS) > 0) {
            throw new IllegalStateException("BFD coverage sync is in progress; re-run once it ends");
        }
        if (jdbcTemplate.queryForList(GET_CONTRACTS_WITH_ACTIVE_V3_JOBS, String.class).contains(contract)) {
            throw new IllegalStateException("Contract " + contract + " has a V3 job in progress; re-run once it ends");
        }

        DistributedLock lock = lockRegistry.obtain(LOCK_NAME_PREFIX + contract);
        if (!lock.tryLock(LOCK_WAIT, LOCK_TTL)) {
            throw new IllegalStateException("Could not acquire " + LOCK_NAME_PREFIX + contract + " within " + LOCK_WAIT
                    + "; moveToHistorical is probably running for it. Re-run later");
        }
        try {
            transactionTemplate.executeWithoutResult(status -> backfillInTransaction(contract, month));
        } finally {
            lock.unlock();
        }
    }

    private void backfillInTransaction(String contract, YearMonth month) {
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        jdbcTemplate.execute("SET LOCAL statement_timeout = 0");
        jdbcTemplate.execute("SET LOCAL work_mem = '" + HISTORY_SUMMARY_WORK_MEM + "'");

        if (count(COUNT_EXTRACT_ROWS, contract, year, monthValue) == 0) {
            throw new IllegalStateException("IDR returned no rows for contract " + contract + " in " + month);
        }

        int rowsInserted = jdbcTemplate.update(INSERT_HISTORICAL, contract, year, monthValue);
        if (rowsInserted == 0 && count(COUNT_COVERAGE_PERIOD, contract, year, monthValue) > 0) {
            log.info("V3 backfill: {} was already restored for contract {}", month, contract);
            return;
        }

        jdbcTemplate.update(DELETE_HISTORY_SUMMARY, contract);
        jdbcTemplate.update(INSERT_HISTORY_SUMMARY, contract);
        jdbcTemplate.update(INSERT_COVERAGE_PERIODS, contract);

        if (count(COUNT_HISTORICAL_ROWS, contract, year, monthValue) == 0
                || count(COUNT_COVERAGE_PERIOD, contract, year, monthValue) == 0) {
            throw new IllegalStateException("Backfill of " + month + " for contract " + contract + " did not verify");
        }
        log.info("V3 backfill: restored {} for contract {}, rowsInserted={}", month, contract, rowsInserted);
    }

    public void dropLoadTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + LOAD_TABLE);
        log.info("V3 backfill: dropped {}", LOAD_TABLE);
    }

    private boolean isTestContract(String contract) {
        return contract.toUpperCase(Locale.ROOT).startsWith("Z");
    }

    private long count(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }
}
