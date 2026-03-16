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
import java.time.LocalDate;
import java.time.ZoneOffset;

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

    private static final String TRUNCATE_SQL = "TRUNCATE TABLE %s";

    private static final String COVERAGE_UPSERT_SQL =
            """
                    INSERT INTO %s (patient_id, contract, year, month, current_mbi)
                    SELECT patient_id, contract, year, month, current_mbi
                    FROM %s
                    ON CONFLICT (patient_id, contract, year, month, current_mbi)
                    DO NOTHING""";

    private static final String HISTORIC_SYNC_SQL =
            """
                    INSERT INTO %s (patient_id, contract, year, month, current_mbi)
                    SELECT coverage.patient_id, coverage.contract, coverage.year, coverage.month, coverage.current_mbi
                    FROM %s coverage
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM %s historic
                        WHERE historic.patient_id = coverage.patient_id
                          AND historic.contract = coverage.contract
                          AND historic.year = coverage.year
                          AND historic.month = coverage.month
                          AND historic.current_mbi = coverage.current_mbi
                    )
                    ON CONFLICT (patient_id, contract, year, month, current_mbi)
                    DO NOTHING""";

    private static final String DELETE_OLD_MONTHS_SQL =
            "DELETE FROM %s\n" +
                    "WHERE make_date(year, month, 1) < (date_trunc('month', CURRENT_DATE) - interval '2 months')::date";

    private static final String COVERAGE_V3_TABLE = "v3.coverage_v3";
    private static final String COVERAGE_V3_HISTORIC_TABLE = "v3.coverage_v3_historic";

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void importWithRetry(String fqtn, String bucket, String key, String region) throws Exception {
        String stagingFqtn = fqtn + "_staging";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            try {
                long before = queryCount(connection, fqtn);
                int stagedRows = executeImport(connection, stagingFqtn, bucket, key, region);
                int upsertRows = upsert(connection, fqtn, stagingFqtn);

                int historicRows = 0;
                //If first day of month
                if (LocalDate.now(ZoneOffset.UTC).getDayOfMonth() == 1) {
                    historicRows = syncToHistoric(connection, fqtn);
                }

                int deletedOldRows = deleteOldCoverageMonths(connection, fqtn);

                truncate(connection, stagingFqtn);
                long after = queryCount(connection, fqtn);

                connection.commit();
                log.info(
                        "Coverage_V3 import success: stagedRows={}, upsertCoverageRows={}, historicRows={}, deletedOldRows={}, before={}, after={}, source=s3://{}/{}",
                        stagedRows, upsertRows, historicRows, deletedOldRows, before, after, bucket, key
                );
            } catch (Exception e) {
                rollback(connection);
                log.error(
                        "CoverageV3 import failed: source=s3://{}/{}, target={}", bucket, key, fqtn, e
                );
                throw e;
            }
            connection.setAutoCommit(true);
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
                String result = rs.getString(1);
                int spaceIndex = result.indexOf(' ');
                int rows = Integer.parseInt(result.substring(0, spaceIndex));
                log.info("Import result: {}", result);
                return rows;
            }
        }
    }

    private int upsert(Connection conn, String targetFqtn, String stagingFqtn) throws Exception {
        String sql = String.format(COVERAGE_UPSERT_SQL, targetFqtn, stagingFqtn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    private int syncToHistoric(Connection conn, String sourceFqtn) throws Exception {
        String sql = String.format(HISTORIC_SYNC_SQL, COVERAGE_V3_HISTORIC_TABLE, sourceFqtn, COVERAGE_V3_HISTORIC_TABLE);
        log.info("----- "+ sql);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            log.info("Starting coverage_v3_historic sync");
            return ps.executeUpdate();
        }
    }

    private int deleteOldCoverageMonths(Connection conn, String fqtn) throws Exception {
        String sql = String.format(DELETE_OLD_MONTHS_SQL, fqtn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            log.info("Starting 3-month data cleanup");
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