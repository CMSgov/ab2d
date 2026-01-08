package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
@Slf4j
public class CoverageV3S3Importer {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.awsRegion}")
    private String region;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.key}")
    private String key;

    @Value("${coverage.import.schema}")
    private String schema;

    @Value("${coverage.import.table}")
    private String table;

    private static final String COLUMNS = "patient_id,contract,year,month,current_mbi,historic_mbis";

    private static final String COPY_OPTIONS =
            "(format csv, header true, null 'NULL')";

    private static final String IMPORT_SQL =
            "SELECT aws_s3.table_import_from_s3(" +
                    " ?, ?, ?, aws_commons.create_s3_uri(?, ?, ?))";

    public void runOnce() throws Exception {
        validateConfig();

        try (S3Client s3 = S3Client.builder().region(Region.of(region)).build()) {
            ensureObjectExists(s3);

            String fqtn = schema + "." + table;
            log.info("Starting import of s3://{}/{} into {}", bucket, key, fqtn);

            importWithRetry(fqtn);
        }
    }

    /**
     * Retry the DB import (3 attempts, exponential backoff).
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void importWithRetry(String fqtn) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            long before = queryCount(conn, fqtn);

            int imported = executeImport(conn, fqtn);

            long after = queryCount(conn, fqtn);

            log.info("Import completed: importedRows={}, before={}, after={}",
                    imported, before, after);
        }
    }

    @Recover
    public void recover(Exception e, String fqtn) throws Exception {
        log.error("Import failed after retries for s3://{}/{} into {}", bucket, key, fqtn, e);
        throw e;
    }

    private void ensureObjectExists(S3Client s3) {
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new IllegalStateException(
                    "S3 object does not exist: s3://" + bucket + "/" + key, e);
        }
    }

    private int executeImport(Connection conn, String fqtn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(IMPORT_SQL)) {
            ps.setString(1, fqtn);
            ps.setString(2, COLUMNS);
            ps.setString(3, COPY_OPTIONS);
            ps.setString(4, bucket);
            ps.setString(5, key);
            ps.setString(6, region);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private long queryCount(Connection conn, String fqtn) throws Exception {
        try (PreparedStatement ps =
                     conn.prepareStatement("SELECT COUNT(*) FROM " + fqtn);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void validateConfig() {
        if (!hasText(bucket)) throw new IllegalArgumentException("app.s3.bucket must be set");
        if (!hasText(key)) throw new IllegalArgumentException("app.s3.key must be set");
        if (!hasText(region)) throw new IllegalArgumentException("app.awsRegion must be set");
        if (!hasText(schema) || !hasText(table)) {
            throw new IllegalArgumentException("coverage.import.schema and coverage.import.table must be set");
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
