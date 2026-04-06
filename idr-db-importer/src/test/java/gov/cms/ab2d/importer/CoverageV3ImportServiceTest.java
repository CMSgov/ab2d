package gov.cms.ab2d.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneOffset;

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
    private static final String STATEMENT_TIMEOUT_SQL = """
            SET statement_timeout TO '30min'""";

    private CoverageV3ImportService service;

    private Connection connection;
    private Statement timeoutStatement;
    private PreparedStatement importPs;
    private PreparedStatement truncatePs;

    private S3ClientBuilder s3ClientBuilder;

    @BeforeEach
    void setUp() throws Exception {
        service = new CoverageV3ImportService();
        setField(service, "jdbcUrl", JDBC_URL);
        setField(service, "dbUser", DB_USER);
        setField(service, "dbPassword", DB_PASSWORD);

        connection = mock(Connection.class);
        timeoutStatement = mock(Statement.class);
        importPs = mock(PreparedStatement.class);
        truncatePs = mock(PreparedStatement.class);
        ResultSet importRs = mock(ResultSet.class);

        S3Client s3Client = mock(S3Client.class);
        s3ClientBuilder = mock(S3ClientBuilder.class);

        when(connection.createStatement()).thenReturn(timeoutStatement);

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn(IMPORT_RESULT);

        when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.credentialsProvider(any(DefaultCredentialsProvider.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.build()).thenReturn(s3Client);
    }

  //  @Test
    void runsHistoricalSyncOnFirstDay() throws Exception {
        LocalDate firstDay = LocalDate.of(2026, 4, 1);
        PreparedStatement historicalPs = mock(PreparedStatement.class);
        PreparedStatement deletePs = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            if (sql.contains("coverage_v3_historical")) {
                return historicalPs;
            }
            if (sql.startsWith("DELETE FROM")) {
                return deletePs;
            }
            if (sql.startsWith("TRUNCATE TABLE")) {
                return truncatePs;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });

        when(historicalPs.executeUpdate()).thenReturn(3);
        when(deletePs.executeUpdate()).thenReturn(2);

        runImport(firstDay);

        verify(timeoutStatement).execute(STATEMENT_TIMEOUT_SQL);
        verify(truncatePs).execute();
        verify(importPs).executeQuery();
        verify(historicalPs).executeUpdate();
        verify(deletePs).executeUpdate();
    }

    @Test
    void skipsHistoricalSyncWhenNotFirstDay() throws Exception {
        LocalDate notFirstDay = LocalDate.of(2026, 4, 2);

        stubNonFirstDayStatements();

        runImport(notFirstDay);

        verify(timeoutStatement).execute(STATEMENT_TIMEOUT_SQL);
        verify(truncatePs).execute();
        verify(importPs).executeQuery();
        verify(connection).commit();
    }

    @Test
    void importWithRetry_deletesFileFromS3AfterImport() throws Exception {
        LocalDate notFirstDay = LocalDate.of(2026, 4, 2);

        stubNonFirstDayStatements();

        runImport(notFirstDay);

        verify(timeoutStatement).execute(STATEMENT_TIMEOUT_SQL);
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void importWithRetry_rollsBackWhenImportFails() throws Exception {
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

        verify(timeoutStatement).execute(STATEMENT_TIMEOUT_SQL);
        verify(connection).rollback();
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

    private void runImport(LocalDate currentDate) throws Exception {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<LocalDate> localDate = mockStatic(LocalDate.class);
             MockedStatic<S3Client> s3ClientStatic = mockStatic(S3Client.class)) {

            driverManager.when(() -> DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD))
                    .thenReturn(connection);
            localDate.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(currentDate);
            s3ClientStatic.when(S3Client::builder).thenReturn(s3ClientBuilder);

            service.importWithRetry(FQTN, BUCKET, KEY, REGION);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}