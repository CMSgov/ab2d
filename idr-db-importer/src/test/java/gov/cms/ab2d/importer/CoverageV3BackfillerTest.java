package gov.cms.ab2d.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.regions.Region;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CoverageV3BackfillerTest {

    private static final String BUCKET = "bucket";
    private static final YearMonth MONTH = YearMonth.of(2026, 6);
    private static final BackfillRequest REQUEST = new BackfillRequest(MONTH, List.of("S4802", "S5601"));
    private static final String KEY = "coverage_v3_backfill_202606.csv";

    private SnowflakeCoverageQueryService queryService;
    private S3CsvWriter s3Writer;
    private CoverageV3ImportService importService;
    private CoverageV3HistoricalBackfill historicalBackfill;

    @BeforeEach
    void setUp() {
        queryService = mock(SnowflakeCoverageQueryService.class);
        s3Writer = mock(S3CsvWriter.class);
        importService = mock(CoverageV3ImportService.class);
        historicalBackfill = mock(CoverageV3HistoricalBackfill.class);
    }

    @Test
    void exportsLoadsAndBackfillsEachContract() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(queryService.open()).thenReturn(connection);
        when(queryService.prepareBackfill(connection, REQUEST)).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        backfiller(queryService).run(REQUEST);

        verify(s3Writer).writeSnowflakeToS3(BUCKET, KEY, resultSet);
        verify(importService).importBackfillWithRetry(BUCKET, KEY, "us-east-1");
        verify(historicalBackfill).backfill("S4802", MONTH);
        verify(historicalBackfill).backfill("S5601", MONTH);
        verify(historicalBackfill).dropLoadTable();
    }

    @Test
    void importsSeededFileWhenSnowflakeIsNotConfigured() throws Exception {
        backfiller(null).run(REQUEST);

        verifyNoInteractions(s3Writer);
        verify(importService).importBackfillWithRetry(BUCKET, KEY, "us-east-1");
    }

    @Test
    void continuesPastAFailedContractThenFails() throws Exception {
        doThrow(new IllegalStateException("job in progress")).when(historicalBackfill).backfill("S4802", MONTH);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> backfiller(null).run(REQUEST));

        assertTrue(e.getMessage().contains("[S4802]"));
        verify(historicalBackfill).backfill("S5601", MONTH);
        verify(historicalBackfill).dropLoadTable();
    }

    @Test
    void doesNotTouchHistoricalWhenTheLoadFails() throws Exception {
        when(importService.importBackfillWithRetry(BUCKET, KEY, "us-east-1"))
                .thenThrow(new RuntimeException("NoSuchKey"));

        assertThrows(RuntimeException.class, () -> backfiller(null).run(REQUEST));

        verify(historicalBackfill, never()).backfill(any(), any());
    }

    @SuppressWarnings("unchecked")
    private CoverageV3Backfiller backfiller(SnowflakeCoverageQueryService service) {
        ObjectProvider<SnowflakeCoverageQueryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        CoverageV3Backfiller backfiller = new CoverageV3Backfiller(provider, s3Writer, importService, historicalBackfill);
        ReflectionTestUtils.setField(backfiller, "bucket", BUCKET);
        ReflectionTestUtils.setField(backfiller, "region", Region.US_EAST_1);
        return backfiller;
    }
}
