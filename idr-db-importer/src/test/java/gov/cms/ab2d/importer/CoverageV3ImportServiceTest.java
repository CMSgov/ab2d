package gov.cms.ab2d.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void importWithRetry_runsHistoricalSyncOnFirstDay() throws Exception {
        LocalDate firstDay = LocalDate.of(2026, 4, 1);

        Connection connection = mock(Connection.class);
        PreparedStatement countPs1 = mock(PreparedStatement.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        PreparedStatement upsertPs = mock(PreparedStatement.class);
        PreparedStatement historicalPs = mock(PreparedStatement.class);
        PreparedStatement deletePs = mock(PreparedStatement.class);
        PreparedStatement truncatePs = mock(PreparedStatement.class);
        PreparedStatement countPs2 = mock(PreparedStatement.class);
        ResultSet countRs1 = mock(ResultSet.class);
        ResultSet importRs = mock(ResultSet.class);
        ResultSet countRs2 = mock(ResultSet.class);

        AtomicInteger countSqlCalls = new AtomicInteger();

        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT COUNT(*) FROM")) {
                return countSqlCalls.getAndIncrement() == 0 ? countPs1 : countPs2;
            }
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            if (sql.contains("FROM " + STAGING_FQTN)) {
                return upsertPs;
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

        when(countPs1.executeQuery()).thenReturn(countRs1);
        when(countRs1.next()).thenReturn(true);
        when(countRs1.getLong(1)).thenReturn(5L);

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn("10 rows imported");

        when(upsertPs.executeUpdate()).thenReturn(7);
        when(historicalPs.executeUpdate()).thenReturn(3);
        when(deletePs.executeUpdate()).thenReturn(2);

        when(countPs2.executeQuery()).thenReturn(countRs2);
        when(countRs2.next()).thenReturn(true);
        when(countRs2.getLong(1)).thenReturn(10L);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<LocalDate> localDate = mockStatic(LocalDate.class)) {

            driverManager.when(() -> DriverManager.getConnection("jdbc:test", "user", "password"))
                    .thenReturn(connection);
            localDate.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(firstDay);

            service.importWithRetry(FQTN, "bucket", "file.csv", "us-east-1");
        }

        verify(historicalPs).executeUpdate();
        verify(deletePs).executeUpdate();
        verify(truncatePs).execute();
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void importWithRetry_skipsHistoricalSyncWhenNotFirstDay() throws Exception {
        LocalDate notFirstDay = LocalDate.of(2026, 4, 2);

        Connection connection = mock(Connection.class);
        PreparedStatement countPs1 = mock(PreparedStatement.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        PreparedStatement upsertPs = mock(PreparedStatement.class);
        PreparedStatement deletePs = mock(PreparedStatement.class);
        PreparedStatement truncatePs = mock(PreparedStatement.class);
        PreparedStatement countPs2 = mock(PreparedStatement.class);
        ResultSet countRs1 = mock(ResultSet.class);
        ResultSet importRs = mock(ResultSet.class);
        ResultSet countRs2 = mock(ResultSet.class);

        AtomicInteger countSqlCalls = new AtomicInteger();

        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT COUNT(*) FROM")) {
                return countSqlCalls.getAndIncrement() == 0 ? countPs1 : countPs2;
            }
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            if (sql.contains("FROM " + STAGING_FQTN)) {
                return upsertPs;
            }
            if (sql.startsWith("DELETE FROM")) {
                return deletePs;
            }
            if (sql.startsWith("TRUNCATE TABLE")) {
                return truncatePs;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });

        when(countPs1.executeQuery()).thenReturn(countRs1);
        when(countRs1.next()).thenReturn(true);
        when(countRs1.getLong(1)).thenReturn(5L);

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getString(1)).thenReturn("10 rows imported");

        when(upsertPs.executeUpdate()).thenReturn(7);
        when(deletePs.executeUpdate()).thenReturn(2);

        when(countPs2.executeQuery()).thenReturn(countRs2);
        when(countRs2.next()).thenReturn(true);
        when(countRs2.getLong(1)).thenReturn(10L);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class);
             MockedStatic<LocalDate> localDate = mockStatic(LocalDate.class)) {

            driverManager.when(() -> DriverManager.getConnection("jdbc:test", "user", "password"))
                    .thenReturn(connection);
            localDate.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(notFirstDay);

            service.importWithRetry(FQTN, "bucket", "file.csv", "us-east-1");
        }

        verify(deletePs).executeUpdate();
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection, never()).prepareStatement(contains("coverage_v3_historical"));
    }

    @Test
    void importWithRetry_rollsBackWhenImportFails() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement countPs = mock(PreparedStatement.class);
        PreparedStatement importPs = mock(PreparedStatement.class);
        ResultSet countRs = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("SELECT COUNT(*) FROM")) {
                return countPs;
            }
            if (sql.startsWith("SELECT aws_s3.table_import_from_s3")) {
                return importPs;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });

        when(countPs.executeQuery()).thenReturn(countRs);
        when(countRs.next()).thenReturn(true);
        when(countRs.getLong(1)).thenReturn(5L);
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