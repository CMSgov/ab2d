package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class CoverageV3S3Importer {

    @Value("${app.awsRegion:us-east-1}")
    private Region region;

    @Value("${app.s3.bucket}")
    private String bucket;

    private final String schema = "v3";

    private final String table = "coverage_v3";


    private final CoverageV3ImportService importService;
 //   private final SnowflakeCoverageQueryService snowflake;
    private final S3CsvWriter s3Writer;

    public CoverageV3S3Importer(S3CsvWriter s3Writer, CoverageV3ImportService importService) {
      //  public CoverageV3S3Importer(SnowflakeCoverageQueryService snowflake, S3CsvWriter s3Writer, CoverageV3ImportService importService) {
   //     this.snowflake = snowflake;
        this.s3Writer = s3Writer;
        this.importService = importService;
    }

    public void runOnce() throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String finalKey = "coverage_v3_" + date + ".csv";

//        // 1) Export from Snowflake -> S3 (streaming, multipart, atomic publish)
//        try (Connection conn = snowflake.open();
//             PreparedStatement ps = snowflake.prepare(conn);
//             ResultSet rs = ps.executeQuery()) {
//
//            log.info("Exporting Snowflake results to s3://{}/{}", bucket, finalKey);
//            s3Writer.writeSnowflakeToS3(bucket, finalKey, rs);
//        }

        String fqtn = schema + "." + table;

        log.info("Starting import of s3://{}/{} into {}", bucket, finalKey, fqtn);
        importService.importWithRetry(fqtn, bucket, finalKey, region.id());
    }

}
