package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

import java.sql.*;

@Component
@Slf4j
public class CoverageV3ImportService {

    private final S3Client s3Client;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private static final String COLUMNS =
            "patient_id,contract,year,month,current_mbi";

    private static final String COPY_OPTIONS =
            "(format csv, header true, null 'NULL')";

    private static final String IMPORT_SQL =
            "SELECT aws_s3.table_import_from_s3(?, ?, ?, aws_commons.create_s3_uri(?, ?, ?))";

    private static final String TRUNCATE_SQL = "TRUNCATE TABLE %s";

    public CoverageV3ImportService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Retryable(
            retryFor = Exception.class,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void importWithRetry(String fqtn, String bucket, String key, String region) throws SQLException {
        String stagingFqtn = fqtn + "_staging";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            connection.setAutoCommit(false);

            try {
                truncate(connection, stagingFqtn);
                verifyFileExists(bucket, key);

                int stagedRows = executeImport(connection, stagingFqtn, bucket, key, region);

                connection.commit();

                try {
                    deleteFileFromS3(bucket, key);
                } catch (Exception e) {
                    log.warn("Failed to delete imported file s3://{}/{}, may need manual cleanup", bucket, key, e);
                }

                log.info(
                        "Coverage_V3 import success: stagedRows={} source=s3://{}/{}",
                        stagedRows, bucket, key
                );
            } catch (Exception e) {
                rollback(connection);
                log.error(
                        "CoverageV3 import failed: source=s3://{}/{}, target={}",
                        bucket, key, stagingFqtn, e
                );
                throw e;
            }
        }
    }

    @Recover
    public void recover(Exception e, String fqtn, String bucket, String key) throws Exception {
        log.error(
                "CoverageV3 import permanently failed after retries: s3://{}/{} → {}",
                bucket, key, fqtn, e
        );
        throw e;
    }

    private void truncate(Connection conn, String fqtn) throws SQLException {
        String sql = String.format(TRUNCATE_SQL, fqtn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private int executeImport(Connection conn, String fqtn, String bucket, String key, String region) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(IMPORT_SQL)) {
            ps.setQueryTimeout(1800);
            ps.setString(1, fqtn);
            ps.setString(2, COLUMNS);
            ps.setString(3, COPY_OPTIONS);
            ps.setString(4, bucket);
            ps.setString(5, key);
            ps.setString(6, region);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                String result = rs.getString(1);
                int spaceIndex = result.indexOf(' ');
                int rows = Integer.parseInt(result.substring(0, spaceIndex));
                log.info("Import result: {}", result);
                return rows;
            }
        }
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ex) {
            log.warn("Rollback failed", ex);
        }
    }

    private void verifyFileExists(String bucket, String key) {
        s3Client.headObject(builder -> builder.bucket(bucket).key(key));
        log.info("Verified file exists in S3 before import: s3://{}/{}", bucket, key);
    }

    private void deleteFileFromS3(String bucket, String key) {
        s3Client.deleteObject(builder -> builder
                .bucket(bucket)
                .key(key)
        );

        log.info("Deleted imported file from S3: s3://{}/{}", bucket, key);
    }
}
