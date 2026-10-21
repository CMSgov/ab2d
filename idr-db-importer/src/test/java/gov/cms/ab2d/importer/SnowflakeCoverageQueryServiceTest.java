package gov.cms.ab2d.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SnowflakeCoverageQueryServiceTest {

    private static final String BASE_URL = "jdbc:snowflake://test-account.snowflakecomputing.com";
    private static final String URL_WITH_QUERY = "jdbc:snowflake://test-account.snowflakecomputing.com/?warehouse=WH1";
    private static final String USER = "test-user";
    private static final String PRIVATE_KEY_PEM = "fake-pem";
    private static final String ROLE = "TEST_ROLE";
    private static final String WAREHOUSE = "TEST_WH";
    private static final String DB = "TEST_DB";
    private static final String SCHEMA = "TEST_SCHEMA";

    private PrivateKey privateKey;
    private SnowflakeCoverageQueryService.PrivateKeyLoader privateKeyLoader;
    private SnowflakeCoverageQueryService service;

    @BeforeEach
    void setUp() {
        privateKey = mock(PrivateKey.class);
        privateKeyLoader = pem -> privateKey;
        service = createService(BASE_URL, privateKeyLoader);
    }

    @Test
    void buildsJdbcUrlWithoutQueryParamsAndPassesProperties() throws Exception {
        Connection connection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenReturn(connection);

            Connection result = service.open();

            assertSame(connection, result);

            driverManager.verify(() -> DriverManager.getConnection(
                    eq(BASE_URL + "?authenticator=SNOWFLAKE_JWT"),
                    argThat(this::hasExpectedProperties)
            ));
        }
    }

    @Test
    void buildsJdbcUrlWithExistingQueryParams() throws Exception {
        SnowflakeCoverageQueryService serviceWithQuery = createService(URL_WITH_QUERY, privateKeyLoader);
        Connection connection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenReturn(connection);

            serviceWithQuery.open();

            driverManager.verify(() -> DriverManager.getConnection(
                    eq(URL_WITH_QUERY + "&authenticator=SNOWFLAKE_JWT"),
                    argThat(this::hasExpectedProperties)
            ));
        }
    }

    @Test
    void throwsWhenPrivateKeyLoaderFails() {
        SnowflakeCoverageQueryService failingService = createService(
                BASE_URL,
                pem -> {
                    throw new IllegalArgumentException("Unsupported private key format");
                }
        );

        assertThrows(IllegalArgumentException.class, failingService::open);
    }

    @Test
    void setsFetchSize() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);

        PreparedStatement result = service.prepare(connection);

        assertSame(statement, result);
        verify(connection).prepareStatement(argThat(this::isExpectedSql));
        verify(statement).setFetchSize(10_000);
    }

    private SnowflakeCoverageQueryService createService(
            String url,
            SnowflakeCoverageQueryService.PrivateKeyLoader loader
    ) {
        return new SnowflakeCoverageQueryService(
                url,
                USER,
                PRIVATE_KEY_PEM,
                ROLE,
                WAREHOUSE,
                DB,
                SCHEMA,
                loader
        );
    }

    private boolean hasExpectedProperties(Properties props) {
        return USER.equals(props.get("user"))
                && privateKey == props.get("privateKey")
                && "SNOWFLAKE_JWT".equals(props.get("authenticator"))
                && ROLE.equals(props.get("role"))
                && WAREHOUSE.equals(props.get("warehouse"))
                && DB.equals(props.get("db"))
                && SCHEMA.equals(props.get("schema"));
    }

    private boolean isExpectedSql(String sql) {
        return sql.contains("WITH month_series AS")
                && sql.contains("FROM TABLE(GENERATOR(ROWCOUNT => 4))")
                && sql.contains("ORDER BY \"patient_id\", \"year\", \"month\"");
    }

    @Test
    void dailySqlKeepsFourMonthWindowAndContractPrefix() {
        assertTrue(SnowflakeCoverageQueryService.SQL.contains("FROM TABLE(GENERATOR(ROWCOUNT => 4))"));
        assertTrue(SnowflakeCoverageQueryService.SQL.contains("LIKE 'S%'"));
        assertFalse(SnowflakeCoverageQueryService.SQL.contains("bene_cntrct_num IN"));
    }

    @Test
    void prepareBackfillBindsMonthAndContracts() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        BackfillRequest request = new BackfillRequest(YearMonth.of(2026, 6), List.of("S4802", "S5601"));
        PreparedStatement result = service.prepareBackfill(connection, request);

        assertSame(statement, result);
        verify(connection).prepareStatement(argThat(sql ->
                sql.contains("SELECT TO_DATE(?) AS month_start")
                        && !sql.contains("GENERATOR")
                        && sql.contains("LIKE 'S%'")
                        && sql.contains("AND elec.bene_cntrct_num IN (?, ?)")
                        && sql.contains("ORDER BY \"patient_id\", \"year\", \"month\"")));
        verify(statement).setString(1, "2026-06-01");
        verify(statement).setString(2, "S4802");
        verify(statement).setString(3, "S5601");
        verify(statement).setFetchSize(10_000);
    }
}
