package gov.cms.ab2d.coverage.query;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Testcontainers
class GetCoveragePeriodsByContractTest {

    @Container
    private static final PostgresTestContainer container = new PostgresTestContainer();

    GetCoveragePeriodsByContract query;

    @BeforeEach
    void setup() {
        query = new GetCoveragePeriodsByContract(container.getDataSource());
    }

    @Test
    void test_expected_periods_for_Z0000()  {
        val periods = query.getCoveragePeriodsForContract("Z0000");
        assertEquals(9, periods.size());
        assertEquals("YearMonthRecord(year=2025, month=6)", periods.get(0).toString());
        assertEquals("YearMonthRecord(year=2025, month=7)", periods.get(1).toString());
        assertEquals("YearMonthRecord(year=2025, month=8)", periods.get(2).toString());
        assertEquals("YearMonthRecord(year=2025, month=9)", periods.get(3).toString());
        assertEquals("YearMonthRecord(year=2025, month=10)", periods.get(4).toString());
        assertEquals("YearMonthRecord(year=2025, month=11)", periods.get(5).toString());
        assertEquals("YearMonthRecord(year=2025, month=12)", periods.get(6).toString());
        assertEquals("YearMonthRecord(year=2026, month=1)", periods.get(7).toString());
        assertEquals("YearMonthRecord(year=2026, month=2)", periods.get(8).toString());
    }

    @Test
    void test_expected_periods_for_Z7777()  {
        val periods = query.getCoveragePeriodsForContract("Z7777");
        assertEquals(3, periods.size());
        assertEquals("YearMonthRecord(year=2025, month=12)", periods.get(0).toString());
        assertEquals("YearMonthRecord(year=2026, month=1)", periods.get(1).toString());
        assertEquals("YearMonthRecord(year=2026, month=2)", periods.get(2).toString());
    }

    @Test
    void test_expected_periods_for_Z8888()  {
        val periods = query.getCoveragePeriodsForContract("Z8888");
        assertEquals(3, periods.size());
        assertEquals("YearMonthRecord(year=2025, month=7)", periods.get(0).toString());
        assertEquals("YearMonthRecord(year=2025, month=8)", periods.get(1).toString());
        assertEquals("YearMonthRecord(year=2025, month=9)", periods.get(2).toString());
    }

    @Test
    void test_expected_periods_for_Z9999()  {
        val periods = query.getCoveragePeriodsForContract("Z9999");
        assertEquals(6, periods.size());
        assertEquals("YearMonthRecord(year=2025, month=9)", periods.get(0).toString());
        assertEquals("YearMonthRecord(year=2025, month=10)", periods.get(1).toString());
        assertEquals("YearMonthRecord(year=2025, month=11)", periods.get(2).toString());
        assertEquals("YearMonthRecord(year=2025, month=12)", periods.get(3).toString());
        assertEquals("YearMonthRecord(year=2026, month=1)", periods.get(4).toString());
        assertEquals("YearMonthRecord(year=2026, month=2)", periods.get(5).toString());
    }

    @Test
    void test_query_for_nonexistent_contract() {
        val periods = query.getCoveragePeriodsForContract("ABC");
        assertTrue(periods.isEmpty());
    }

}
