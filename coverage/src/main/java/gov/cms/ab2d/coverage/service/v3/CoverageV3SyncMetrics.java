package gov.cms.ab2d.coverage.service.v3;

import com.timgroup.statsd.StatsDClient;
import gov.cms.ab2d.coverage.service.v3.audit.CoverageV3AuditAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.coverage.service.v3.audit.CoverageV3AuditAction.COPY_FROM_STAGING;
import static gov.cms.ab2d.coverage.service.v3.audit.CoverageV3AuditAction.COPY_TO_HISTORICAL;

@Slf4j
@Component
public class CoverageV3SyncMetrics {

    static final String IMPORT_ROWS_STAGED = "coverage.v3.import.rows_staged";
    static final String IMPORT_ROWS_BEFORE = "coverage.v3.import.rows_before";
    static final String IMPORT_ROWS_AFTER = "coverage.v3.import.rows_after";
    static final String IMPORT_ROWS_DELTA = "coverage.v3.import.rows_delta";
    static final String IMPORT_COMPLETED = "coverage.v3.import.completed";

    static final String HISTORICAL_ROWS_MOVED = "coverage.v3.historical.rows_moved";
    static final String HISTORICAL_ROWS_DELETED = "coverage.v3.historical.rows_deleted";
    static final String HISTORICAL_ROWS_DELTA = "coverage.v3.historical.rows_delta";
    static final String HISTORICAL_COMPLETED = "coverage.v3.historical.completed";

    private final String executionEnv;
    private final StatsDClient statsDClient;

    public CoverageV3SyncMetrics(
            @Value("${execution.env}") String executionEnv,
            ObjectProvider<StatsDClient> statsDClientProvider) {
        this.executionEnv = executionEnv;
        this.statsDClient = statsDClientProvider.getIfAvailable();
        if (this.statsDClient == null) {
            log.info("No StatsDClient bean available; Coverage V3 sync metrics are disabled (no-op)");
        }
    }

    /**
     * Record the outcome of a {@code COPY_FROM_STAGING} sync (staging table into the recent coverage
     * table).
     */
    public void recordImport(CoverageV3SyncSource source, String contract, CoverageV3SyncResult result,
                             Integer rowsStaged, Integer rowsBefore, Integer rowsAfter) {
        String[] tags = buildTags(COPY_FROM_STAGING, source, contract, result);
        gauge(IMPORT_ROWS_STAGED, rowsStaged, tags);
        gauge(IMPORT_ROWS_BEFORE, rowsBefore, tags);
        gauge(IMPORT_ROWS_AFTER, rowsAfter, tags);
        if (rowsBefore != null && rowsAfter != null) {
            gauge(IMPORT_ROWS_DELTA, rowsAfter - rowsBefore, tags);
        }
        increment(IMPORT_COMPLETED, tags);
    }

    /**
     * Record the outcome of a {@code COPY_TO_HISTORICAL} sync (recent coverage table into the
     * historical table).
     */
    public void recordHistorical(CoverageV3SyncSource source, String contract, CoverageV3SyncResult result,
                                 Integer rowsMoved, Integer rowsDeleted) {
        String[] tags = buildTags(COPY_TO_HISTORICAL, source, contract, result);
        gauge(HISTORICAL_ROWS_MOVED, rowsMoved, tags);
        gauge(HISTORICAL_ROWS_DELETED, rowsDeleted, tags);
        if (rowsMoved != null) {
            // Rows moved into the historical table are the net delta for this operation.
            gauge(HISTORICAL_ROWS_DELTA, rowsMoved, tags);
        }
        increment(HISTORICAL_COMPLETED, tags);
    }

    private void gauge(String aspect, Integer value, String[] tags) {
        if (statsDClient != null && value != null) {
            statsDClient.gauge(aspect, value, tags);
        }
    }

    private void increment(String aspect, String[] tags) {
        if (statsDClient != null) {
            statsDClient.increment(aspect, tags);
        }
    }

    private String[] buildTags(CoverageV3AuditAction operation, CoverageV3SyncSource source,
                               String contract, CoverageV3SyncResult result) {
        List<String> tags = new ArrayList<>();
        tags.add("environment:" + executionEnv);
        tags.add("operation:" + operation.name().toLowerCase());
        if (contract != null) {
            tags.add("contract:" + contract);
        }
        if (source != null) {
            tags.add("source:" + source.name().toLowerCase());
        }
        if (result != null) {
            tags.add("result:" + result.name().toLowerCase());
        }
        return tags.toArray(new String[0]);
    }
}
