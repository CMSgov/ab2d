package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
@Slf4j
public class CoverageV3ImportService {

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

    private static final String CREATE_STAGING_SQL =
            "CREATE TABLE IF NOT EXISTS %s (LIKE %s INCLUDING DEFAULTS INCLUDING IDENTITY INCLUDING GENERATED)";

    private static final String TRUNCATE_SQL =
            "TRUNCATE TABLE %s";

    private static final String UPSERT_SQL =
            "INSERT INTO %s (patient_id, contract, year, month, current_mbi)\n" +
                    "SELECT patient_id, contract, year, month, current_mbi\n" +
                    "FROM %s\n" +
                    "ON CONFLICT (patient_id, contract, year, month, current_mbi)\n" +
                    "DO NOTHING";

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void importWithRetry(String fqtn, String bucket, String key, String region) throws Exception {
        String stagingFqtn = fqtn + "_staging";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            try {
                ensureStagingTable(conn, stagingFqtn, fqtn);
                truncate(conn, stagingFqtn);
                long before = queryCount(conn, fqtn);
                int stagedRows = executeImport(conn, stagingFqtn, bucket, key, region);
                int upsertRows = upsert(conn, fqtn, stagingFqtn);
                truncate(conn, stagingFqtn);
                long after = queryCount(conn, fqtn);
                conn.commit();
                log.info(
                        "CoverageV3 import success: stagedRows={}, upsertRows={}, before={}, after={}, source=s3://{}/{}",
                        stagedRows, upsertRows, before, after, bucket, key
                );
            } catch (Exception e) {
                rollback(conn);
                log.error(
                        "CoverageV3 import failed: source=s3://{}/{}, target={}",
                        bucket, key, fqtn, e
                );
                throw e;
            }
            conn.setAutoCommit(true);
        }
    }

    @Recover
    public void recover(Exception e, String fqtn, String bucket, String key, String region) throws Exception {
        log.error(
                "CoverageV3 import permanently failed after retries: s3://{}/{} → {}",
                bucket, key, fqtn, e
        );
        throw e;
    }

    private void ensureStagingTable(Connection conn, String stagingFqtn, String targetFqtn) throws Exception {
        String sql = String.format(CREATE_STAGING_SQL, stagingFqtn, targetFqtn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private void truncate(Connection conn, String fqtn) throws Exception {
        String sql = String.format(TRUNCATE_SQL, fqtn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private int executeImport(Connection conn, String fqtn, String bucket, String key, String region) throws Exception {
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

    private int upsert(Connection conn, String targetFqtn, String stagingFqtn) throws Exception {
        String sql = String.format(UPSERT_SQL, targetFqtn, stagingFqtn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    private long queryCount(Connection conn, String fqtn) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + fqtn;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ex) {
            log.warn("Rollback failed", ex);
        }
    }
}