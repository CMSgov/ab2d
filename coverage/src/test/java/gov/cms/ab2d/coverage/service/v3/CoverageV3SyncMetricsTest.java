package gov.cms.ab2d.coverage.service.v3;

import com.timgroup.statsd.StatsDClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.HISTORICAL_COMPLETED;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.HISTORICAL_ROWS_DELETED;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.HISTORICAL_ROWS_DELTA;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.HISTORICAL_ROWS_MOVED;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.IMPORT_COMPLETED;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.IMPORT_ROWS_AFTER;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.IMPORT_ROWS_BEFORE;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.IMPORT_ROWS_DELTA;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncMetrics.IMPORT_ROWS_STAGED;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.SYNC_SUCCESSFUL_FOR_CONTRACT;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncSource.CRON_JOB;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverageV3SyncMetricsTest {

    private static final String ENV = "ab2d-dev";

    @Mock
    private StatsDClient statsDClient;

    @Mock
    private ObjectProvider<StatsDClient> statsDClientProvider;

    private CoverageV3SyncMetrics newMetrics() {
        when(statsDClientProvider.getIfAvailable()).thenReturn(statsDClient);
        return new CoverageV3SyncMetrics(ENV, statsDClientProvider);
    }

    @Test
    void recordImport_emitsAllRowCountGaugesAndCompletionCounter() {
        CoverageV3SyncMetrics metrics = newMetrics();

        metrics.recordImport(CRON_JOB, "Z1234", SYNC_SUCCESSFUL_FOR_CONTRACT, 100, 40, 100);

        String[] tags = {
                "environment:ab2d-dev",
                "operation:copy_from_staging",
                "contract:Z1234",
                "source:cron_job",
                "result:sync_successful_for_contract"
        };
        verify(statsDClient).gauge(IMPORT_ROWS_STAGED, 100L, tags);
        verify(statsDClient).gauge(IMPORT_ROWS_BEFORE, 40L, tags);
        verify(statsDClient).gauge(IMPORT_ROWS_AFTER, 100L, tags);
        verify(statsDClient).gauge(IMPORT_ROWS_DELTA, 60L, tags);
        verify(statsDClient).increment(IMPORT_COMPLETED, tags);
    }

    @Test
    void recordImport_withNullBeforeAndAfter_skipsGaugesButStillCounts() {
        CoverageV3SyncMetrics metrics = newMetrics();

        metrics.recordImport(CRON_JOB, "Z1234", UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT, 100, null, null);

        String[] tags = {
                "environment:ab2d-dev",
                "operation:copy_from_staging",
                "contract:Z1234",
                "source:cron_job",
                "result:unable_to_acquire_lock_for_contract"
        };
        verify(statsDClient).gauge(IMPORT_ROWS_STAGED, 100L, tags);
        verify(statsDClient).increment(IMPORT_COMPLETED, tags);
    }

    @Test
    void recordHistorical_emitsMovedDeletedDeltaAndCompletionCounter() {
        CoverageV3SyncMetrics metrics = newMetrics();

        metrics.recordHistorical(CRON_JOB, "Z1234", SYNC_SUCCESSFUL_FOR_CONTRACT, 25, 25);

        String[] tags = {
                "environment:ab2d-dev",
                "operation:copy_to_historical",
                "contract:Z1234",
                "source:cron_job",
                "result:sync_successful_for_contract"
        };
        verify(statsDClient).gauge(HISTORICAL_ROWS_MOVED, 25L, tags);
        verify(statsDClient).gauge(HISTORICAL_ROWS_DELETED, 25L, tags);
        verify(statsDClient).gauge(HISTORICAL_ROWS_DELTA, 25L, tags);
        verify(statsDClient).increment(HISTORICAL_COMPLETED, tags);
    }

    @Test
    void noStatsDClient_isSafeNoOp() {
        when(statsDClientProvider.getIfAvailable()).thenReturn(null);
        CoverageV3SyncMetrics metrics = new CoverageV3SyncMetrics(ENV, statsDClientProvider);

        assertThatCode(() -> {
            metrics.recordImport(CRON_JOB, "Z1234", SYNC_SUCCESSFUL_FOR_CONTRACT, 100, 40, 100);
            metrics.recordHistorical(CRON_JOB, "Z1234", SYNC_SUCCESSFUL_FOR_CONTRACT, 25, 25);
        }).doesNotThrowAnyException();

        verifyNoInteractions(statsDClient);
    }
}
