package gov.cms.ab2d.audit;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
class AuditTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();

    @BeforeAll
    static void createJobTable() throws SQLException {
        // need to create the job table, otherwise the event handler will fail
        try (Connection connection = DriverManager.getConnection(System.getProperty("DB_URL"), System.getProperty("DB_USERNAME"), System.getProperty("DB_PASSWORD"));
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS job (job_uuid VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL)");
        }
    }

    @Test
    void
    auditInvoke() throws IOException {
        Properties properties = PropertiesUtil.loadProps();
        File dir = new File(properties.getProperty("AB2D_EFS_MOUNT") + UUID.randomUUID());
        dir.mkdirs();
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        System.setProperty("audit_files_ttl_hours", "-1");
        AuditEventHandler eventHandler = new AuditEventHandler();
        assertDoesNotThrow(() -> {
            eventHandler.handleRequest(null, System.out, new TestContext());
        });
    }

    @Test
    void auditInvokeNoDelete() throws IOException {
        Properties properties = PropertiesUtil.loadProps();
        File dir = new File(properties.getProperty("AB2D_EFS_MOUNT") + UUID.randomUUID());
        dir.mkdirs();
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        System.setProperty("audit_files_ttl_hours", "1");
        AuditEventHandler eventHandler = new AuditEventHandler();
        assertDoesNotThrow(() -> {
            eventHandler.handleRequest(null, System.out, new TestContext());
        });
    }


}
