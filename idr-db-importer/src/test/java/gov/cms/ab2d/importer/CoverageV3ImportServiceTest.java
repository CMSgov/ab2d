package gov.cms.ab2d.importer;

import gov.cms.ab2d.common.util.DatadogSpans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CoverageV3ImportServiceTest {

    private static final String FQTN = "v3.coverage_v3";
    private static final String JDBC_URL = "jdbc:test";
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "password";
    private static final String BUCKET = "bucket";
    private static final String KEY = "file.csv";
    private static final String REGION = "us-east-1";
    private static final String IMPORT_RESULT = "10 rows imported";

    private S3Client s3Client;
    private CoverageV3ImportService service;

    private Connection connection;
    private PreparedStatement importPs;
    private PreparedStatement truncatePs;

    @BeforeEach
    void setUp() throws Exception {
        s3Client = mock(S3Client.class);
        service = new CoverageV3ImportService(s3Client);
        setField(service, "jdbcUrl", JDBC_URL);
        setField(service, "dbUser", DB_USER);
        setField(service, "dbPassword", DB_PASSWORD);

        connection = mock(Connection.class);
        Statement timeoutStatement = mock(Statement.class);
        importPs = mock(PreparedStatement.class);
        truncatePs = mock(PreparedStatement.class);
        ResultSet importRs = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(timeoutStatement);

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn(IMPORT_RESULT);
    }

    @Test
    void deletesFileFromS3AfterImport() throws Exception {
        stubNonFirstDayStatements();

        runImport();

        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(s3Client).deleteObject(any(Consumer.class));
    }

    @Test
    void rollsBackWhenImportFails() throws Exception {
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            if (sql.startsWith("TRUNCATE TABLE")) {
                return truncatePs;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });

        when(importPs.executeQuery()).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD))
                    .thenReturn(connection);

            assertThrows(RuntimeException.class,
                    () -> service.importWithRetry(FQTN, BUCKET, KEY, REGION));
        }

        verify(connection).rollback();
    }

    @Test
    void tagsSpanAndRecordsStagedRowsOnSuccess() throws Exception {
        stubNonFirstDayStatements();

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<DatadogSpans> spans = mockStatic(DatadogSpans.class)) {
            driverManager.when(() -> DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD))
                    .thenReturn(connection);

            service.importWithRetry(FQTN, BUCKET, KEY, REGION);

            spans.verify(() -> DatadogSpans.setTag("component", "idr"));
            spans.verify(() -> DatadogSpans.setTag("target_table", FQTN + "_staging"));
            spans.verify(() -> DatadogSpans.setMetric("idr.staged_rows", 10L));
        }
    }

    @Test
    void marksSpanErroredWhenImportFails() throws Exception {
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            if (sql.startsWith("TRUNCATE TABLE")) {
                return truncatePs;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });

        when(importPs.executeQuery()).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<DatadogSpans> spans = mockStatic(DatadogSpans.class)) {
            driverManager.when(() -> DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD))
                    .thenReturn(connection);

            assertThrows(RuntimeException.class,
                    () -> service.importWithRetry(FQTN, BUCKET, KEY, REGION));

            spans.verify(() -> DatadogSpans.markError(any(RuntimeException.class)));
        }
    }

    private void stubNonFirstDayStatements() throws Exception {
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            if (sql.startsWith("TRUNCATE TABLE")) {
                return truncatePs;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });
    }

    private void runImport() throws Exception {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD))
                    .thenReturn(connection);

            service.importWithRetry(FQTN, BUCKET, KEY, REGION);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}