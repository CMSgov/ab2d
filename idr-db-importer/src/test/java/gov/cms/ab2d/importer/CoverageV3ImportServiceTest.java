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
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CoverageV3ImportServiceTest {

    private static final String FQTN = "v3.coverage_v3";
    private static final String STAGING_FQTN = "v3.coverage_v3_staging";

    private CoverageV3ImportService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new CoverageV3ImportService();
        setField(service, "jdbcUrl", "jdbc:test");
        setField(service, "dbUser", "user");
        setField(service, "dbPassword", "password");
    }

    @Test
    void runsHistoricalSyncOnFirstDay() throws Exception {
        LocalDate firstDay = LocalDate.of(2026, 4, 1);

        Connection connection = mock(Connection.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        PreparedStatement historicalPs = mock(PreparedStatement.class);
        PreparedStatement deletePs = mock(PreparedStatement.class);
        PreparedStatement truncatePs = mock(PreparedStatement.class);
        ResultSet importRs = mock(ResultSet.class);

        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

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

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn("10 rows imported");

        when(historicalPs.executeUpdate()).thenReturn(3);
        when(deletePs.executeUpdate()).thenReturn(2);

        when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.credentialsProvider(any(DefaultCredentialsProvider.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.build()).thenReturn(s3Client);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<LocalDate> localDate = mockStatic(LocalDate.class);
             MockedStatic<S3Client> s3ClientStatic = mockStatic(S3Client.class)) {

            driverManager.when(() -> DriverManager.getConnection("jdbc:test", "user", "password"))
                    .thenReturn(connection);
            localDate.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(firstDay);
            s3ClientStatic.when(S3Client::builder).thenReturn(s3ClientBuilder);

            service.importWithRetry(FQTN, "bucket", "file.csv", "us-east-1");
        }

        verify(truncatePs).execute();
        verify(importPs).executeQuery();
        verify(s3Client).deleteObject(any(java.util.function.Consumer.class));
        verify(historicalPs).executeUpdate();
        verify(deletePs).executeUpdate();
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void skipsHistoricalSyncWhenNotFirstDay() throws Exception {
        LocalDate notFirstDay = LocalDate.of(2026, 4, 2);

        Connection connection = mock(Connection.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        PreparedStatement truncatePs = mock(PreparedStatement.class);
        ResultSet importRs = mock(ResultSet.class);

        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

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

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn("10 rows imported");

        when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.credentialsProvider(any(DefaultCredentialsProvider.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.build()).thenReturn(s3Client);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<LocalDate> localDate = mockStatic(LocalDate.class);
             MockedStatic<S3Client> s3ClientStatic = mockStatic(S3Client.class)) {

            driverManager.when(() -> DriverManager.getConnection("jdbc:test", "user", "password"))
                    .thenReturn(connection);
            localDate.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(notFirstDay);
            s3ClientStatic.when(S3Client::builder).thenReturn(s3ClientBuilder);

            service.importWithRetry(FQTN, "bucket", "file.csv", "us-east-1");
        }

        verify(truncatePs).execute();
        verify(importPs).executeQuery();
        verify(s3Client).deleteObject(any(java.util.function.Consumer.class));
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection, never()).prepareStatement(contains("coverage_v3_historical"));
        verify(connection, never()).prepareStatement(startsWith("DELETE FROM"));
    }

    @Test
    void importWithRetry_deletesFileFromS3AfterImport() throws Exception {
        LocalDate notFirstDay = LocalDate.of(2026, 4, 2);

        Connection connection = mock(Connection.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        PreparedStatement truncatePs = mock(PreparedStatement.class);
        ResultSet importRs = mock(ResultSet.class);

        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

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

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn("10 rows imported");

        when(s3ClientBuilder.region(Region.of("us-east-1"))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.credentialsProvider(any(DefaultCredentialsProvider.class))).thenReturn(s3ClientBuilder);
        when(s3ClientBuilder.build()).thenReturn(s3Client);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<LocalDate> localDate = mockStatic(LocalDate.class);
             MockedStatic<S3Client> s3ClientStatic = mockStatic(S3Client.class)) {

            driverManager.when(() -> DriverManager.getConnection("jdbc:test", "user", "password"))
                    .thenReturn(connection);
            localDate.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(notFirstDay);
            s3ClientStatic.when(S3Client::builder).thenReturn(s3ClientBuilder);

            service.importWithRetry(FQTN, "bucket", "file.csv", "us-east-1");
        }

        verify(s3Client).deleteObject(any(java.util.function.Consumer.class));
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void importWithRetry_rollsBackWhenImportFails() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        PreparedStatement truncatePs = mock(PreparedStatement.class);

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
            driverManager.when(() -> DriverManager.getConnection("jdbc:test", "user", "password"))
                    .thenReturn(connection);

            assertThrows(RuntimeException.class,
                    () -> service.importWithRetry(FQTN, "bucket", "file.csv", "us-east-1"));
        }

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}