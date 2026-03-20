package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class CoverageV3S3Importer {

    @Value("${app.awsRegion:us-east-1}")
    private Region region;

    @Value("${app.s3.bucket}")
    private String bucket;

    private final CoverageV3ImportService importService;
    private final SnowflakeCoverageQueryService coverageQueryService;
    private final S3CsvWriter s3Writer;

    public CoverageV3S3Importer(
            ObjectProvider<SnowflakeCoverageQueryService> coverageQueryServiceProvider,
            S3CsvWriter s3Writer,
            CoverageV3ImportService importService
    ) {
        this.coverageQueryService = coverageQueryServiceProvider.getIfAvailable();
        this.s3Writer = s3Writer;
        this.importService = importService;
    }

    public void runOnce() throws Exception {
        if (coverageQueryService == null) {
            log.info("Snowflake query service is not configured for this environment. Skipping coverage_v3 export and import.");
            return;
        }

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String finalKey = "coverage_v3_" + date + ".csv";

        try (Connection conn = coverageQueryService.open();
             PreparedStatement ps = coverageQueryService.prepare(conn);
             ResultSet rs = ps.executeQuery()) {

            log.info("Exporting Snowflake results to s3://{}/{}", bucket, finalKey);
            s3Writer.writeSnowflakeToS3(bucket, finalKey, rs);
        }

        String fqtn = "v3.coverage_v3";
        log.info("Starting import of s3://{}/{} into {}", bucket, finalKey, fqtn);
        importService.importWithRetry(fqtn, bucket, finalKey, region.id());
    }
}