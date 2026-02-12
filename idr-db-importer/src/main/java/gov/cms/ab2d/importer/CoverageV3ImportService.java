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

    private static final String COLUMNS = "patient_id,contract,year,month,current_mbi,historic_mbis";
    private static final String COPY_OPTIONS = "(format csv, header true, null 'NULL')";
    private static final String IMPORT_SQL =
            "SELECT aws_s3.table_import_from_s3(?, ?, ?, aws_commons.create_s3_uri(?, ?, ?))";

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void importWithRetry(String fqtn, String bucket, String key, String region) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            long before = queryCount(conn, fqtn);

            int imported = executeImport(conn, fqtn, bucket, key, region);

            long after = queryCount(conn, fqtn);

            log.info("Import completed: importedRows={}, before={}, after={}", imported, before, after);
        }
    }

    @Recover
    public void recover(Exception e, String fqtn, String bucket, String key, String region) throws Exception {
        log.error("Import failed after retries for s3://{}/{} into {}", bucket, key, fqtn, e);
        throw e;
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

    private long queryCount(Connection conn, String fqtn) throws Exception {
        String sqlStatement = "SELECT COUNT(*) FROM " + fqtn;
        try (PreparedStatement ps = conn.prepareStatement(sqlStatement);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}

