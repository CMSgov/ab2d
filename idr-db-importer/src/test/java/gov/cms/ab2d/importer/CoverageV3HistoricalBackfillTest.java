package gov.cms.ab2d.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.YearMonth;
import java.util.List;

import static gov.cms.ab2d.importer.CoverageV3HistoricalBackfill.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoverageV3HistoricalBackfillTest {

    private static final String CONTRACT = "S4802";
    private static final YearMonth MONTH = YearMonth.of(2026, 6);

    private JdbcTemplate jdbcTemplate;
    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;
    private JdbcLockRegistry lockRegistry;
    private DistributedLock lock;
    private CoverageV3HistoricalBackfill backfill;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate = mock(JdbcTemplate.class);
        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(TransactionStatus.class);
        lockRegistry = mock(JdbcLockRegistry.class);
        lock = mock(DistributedLock.class);

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(lockRegistry.obtain(LOCK_NAME_PREFIX + CONTRACT)).thenReturn(lock);
        when(lock.tryLock(LOCK_WAIT, LOCK_TTL)).thenReturn(true);
        when(jdbcTemplate.queryForList(GET_CONTRACTS_WITH_ACTIVE_V3_JOBS, String.class)).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(eq(QUERY_BFD_COVERAGE_SYNC_IN_PROGRESS), eq(Long.class), any(Object[].class))).thenReturn(0L);

        backfill = new CoverageV3HistoricalBackfill(jdbcTemplate, transactionManager, lockRegistry);
    }

    @Test
    void insertsMonthAndRebuildsSummaryUnderTheWorkersLock() throws Exception {
        stubCounts(100L, 1L);
        when(jdbcTemplate.update(contains("INSERT INTO v3.coverage_v3_historical"), eq(CONTRACT), eq(2026), eq(6))).thenReturn(100);

        backfill.backfill(CONTRACT, MONTH);

        verify(jdbcTemplate).execute("SET LOCAL statement_timeout = 0");
        verify(jdbcTemplate).update(contains("FROM " + LOAD_TABLE + " b"), eq(CONTRACT), eq(2026), eq(6));
        verify(jdbcTemplate).update("DELETE FROM v3.coverage_v3_history_summary WHERE contract = ?", CONTRACT);
        verify(jdbcTemplate).update(contains("INSERT INTO v3.coverage_v3_history_summary\n"), eq(CONTRACT));
        verify(jdbcTemplate).update(contains("INSERT INTO v3.coverage_v3_history_summary_coverage_periods"), eq(CONTRACT));
        verify(transactionManager).commit(transactionStatus);
        verify(lock).unlock();
    }

    @Test
    void skipsSummaryRebuildWhenMonthIsAlreadyArchived() throws Exception {
        stubCounts(100L, 1L);
        when(jdbcTemplate.update(contains("INSERT INTO v3.coverage_v3_historical"), eq(CONTRACT), eq(2026), eq(6))).thenReturn(0);

        backfill.backfill(CONTRACT, MONTH);

        verify(jdbcTemplate, never()).update(startsWith("DELETE FROM v3.coverage_v3_history_summary"), any(Object[].class));
        verify(lock).unlock();
    }

    @Test
    void rebuildsSummaryWhenRowsExistButThePeriodIsMissing() throws Exception {
        // A run that died between the historical insert and the summary rebuild.
        when(jdbcTemplate.queryForObject(contains("FROM " + LOAD_TABLE), eq(Long.class), any(Object[].class))).thenReturn(100L);
        when(jdbcTemplate.queryForObject(eq(COUNT_COVERAGE_PERIOD), eq(Long.class), any(Object[].class))).thenReturn(0L, 1L);
        when(jdbcTemplate.queryForObject(contains("FROM v3.coverage_v3_historical WHERE"), eq(Long.class), any(Object[].class))).thenReturn(100L);
        when(jdbcTemplate.update(contains("INSERT INTO v3.coverage_v3_historical"), eq(CONTRACT), eq(2026), eq(6))).thenReturn(0);

        backfill.backfill(CONTRACT, MONTH);

        verify(jdbcTemplate).update("DELETE FROM v3.coverage_v3_history_summary WHERE contract = ?", CONTRACT);
    }

    @Test
    void failsWhenContractIsMissingFromExtract() throws Exception {
        stubCounts(0L, 0L);

        assertThrows(IllegalStateException.class, () -> backfill.backfill(CONTRACT, MONTH));

        verify(jdbcTemplate, never()).update(contains("INSERT INTO v3.coverage_v3_historical"), any(Object[].class));
    }

    @Test
    void rollsBackWhenVerificationFails() throws Exception {
        when(jdbcTemplate.queryForObject(contains("FROM " + LOAD_TABLE), eq(Long.class), any(Object[].class))).thenReturn(100L);
        when(jdbcTemplate.queryForObject(eq(COUNT_COVERAGE_PERIOD), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(contains("FROM v3.coverage_v3_historical WHERE"), eq(Long.class), any(Object[].class))).thenReturn(100L);
        when(jdbcTemplate.update(contains("INSERT INTO v3.coverage_v3_historical"), eq(CONTRACT), eq(2026), eq(6))).thenReturn(100);

        assertThrows(IllegalStateException.class, () -> backfill.backfill(CONTRACT, MONTH));

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
        verify(lock).unlock();
    }

    @Test
    void leavesTestContractAlone() {
        assertThrows(IllegalStateException.class, () -> backfill.backfill("Z0001", MONTH));

        verifyNoInteractions(lockRegistry, transactionManager);
    }

    @Test
    void waitsForBfdCoverageSync() {
        when(jdbcTemplate.queryForObject(eq(QUERY_BFD_COVERAGE_SYNC_IN_PROGRESS), eq(Long.class), any(Object[].class))).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> backfill.backfill(CONTRACT, MONTH));

        verifyNoInteractions(lockRegistry, transactionManager);
    }

    @Test
    void transactionTimesOutBeforeTheLockExpires() throws Exception {
        stubCounts(100L, 1L);

        backfill.backfill(CONTRACT, MONTH);

        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertEquals(TRANSACTION_TIMEOUT.toSeconds(), definition.getValue().getTimeout());
        assertTrue(TRANSACTION_TIMEOUT.compareTo(LOCK_TTL) < 0);
    }

    @Test
    void leavesContractWithActiveJobAlone() {
        when(jdbcTemplate.queryForList(GET_CONTRACTS_WITH_ACTIVE_V3_JOBS, String.class)).thenReturn(List.of(CONTRACT));

        assertThrows(IllegalStateException.class, () -> backfill.backfill(CONTRACT, MONTH));

        verifyNoInteractions(lockRegistry, transactionManager);
    }

    @Test
    void givesUpWhenTheWorkerHoldsTheLock() throws Exception {
        when(lock.tryLock(LOCK_WAIT, LOCK_TTL)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> backfill.backfill(CONTRACT, MONTH));

        verifyNoInteractions(transactionManager);
        verify(lock, never()).unlock();
    }

    @Test
    void dropsLoadTable() {
        backfill.dropLoadTable();

        verify(jdbcTemplate).execute("DROP TABLE IF EXISTS " + LOAD_TABLE);
    }

    private void stubCounts(long extractRows, long periodRows) {
        when(jdbcTemplate.queryForObject(contains("FROM " + LOAD_TABLE), eq(Long.class), any(Object[].class))).thenReturn(extractRows);
        when(jdbcTemplate.queryForObject(eq(COUNT_COVERAGE_PERIOD), eq(Long.class), any(Object[].class))).thenReturn(periodRows);
        when(jdbcTemplate.queryForObject(contains("FROM v3.coverage_v3_historical WHERE"), eq(Long.class), any(Object[].class))).thenReturn(extractRows);
    }
}
