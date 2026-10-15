package gov.cms.ab2d.importer;

import datadog.trace.api.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.importer.CoverageV3HistoricalBackfill.LOAD_TABLE;

@Component
@Slf4j
public class CoverageV3Backfiller {

    @Value("${app.awsRegion:us-east-1}")
    private Region region;

    @Value("${app.s3.bucket}")
    private String bucket;

    private final SnowflakeCoverageQueryService coverageQueryService;
    private final S3CsvWriter s3Writer;
    private final CoverageV3ImportService importService;
    private final CoverageV3HistoricalBackfill historicalBackfill;

    public CoverageV3Backfiller(
            ObjectProvider<SnowflakeCoverageQueryService> coverageQueryServiceProvider,
            S3CsvWriter s3Writer,
            CoverageV3ImportService importService,
            CoverageV3HistoricalBackfill historicalBackfill
    ) {
        this.coverageQueryService = coverageQueryServiceProvider.getIfAvailable();
        this.s3Writer = s3Writer;
        this.importService = importService;
        this.historicalBackfill = historicalBackfill;
    }

    @Trace(operationName = "ab2d.idr.backfill")
    public void run(BackfillRequest request) throws Exception {
        String key = request.s3Key();
        log.info("V3 backfill started: month={} contracts={}", request.month(), request.contracts());

        if (coverageQueryService != null) {
            try (Connection conn = coverageQueryService.open();
                 PreparedStatement ps = coverageQueryService.prepareBackfill(conn, request);
                 ResultSet rs = ps.executeQuery()) {

                log.info("Exporting Snowflake results to s3://{}/{}", bucket, key);
                s3Writer.writeSnowflakeToS3(bucket, key, rs);
            }
        } else {
            log.info("Snowflake query service is not configured for this environment. Expecting s3://{}/{} to be seeded.",
                    bucket, key);
        }

        int loadedRows = importService.importBackfillWithRetry(bucket, key, region.id());
        log.info("V3 backfill: loaded {} rows into {}", loadedRows, LOAD_TABLE);

        List<String> failed = new ArrayList<>();
        try {
            for (String contract : request.contracts()) {
                try {
                    historicalBackfill.backfill(contract, request.month());
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("V3 backfill of {} failed for contract {}", request.month(), contract, e);
                    failed.add(contract);
                }
            }
        } finally {
            historicalBackfill.dropLoadTable();
        }

        if (!failed.isEmpty()) {
            throw new IllegalStateException("Backfill of " + request.month() + " did not complete for " + failed
                    + "; the other contracts are committed and a re-run is safe");
        }
    }
}
