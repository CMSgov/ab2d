package gov.cms.ab2d.coverage.query;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static gov.cms.ab2d.coverage.query.TempTestUtils.devDataSource;
import static gov.cms.ab2d.coverage.util.CoverageV3Utils.*;
import static org.junit.Assert.assertEquals;

@Testcontainers
class CountBeneficiariesByCoveragePeriodsTest {

    @Container
    private static final PostgresTestContainer container = new PostgresTestContainer();

    static final ZonedDateTime START_TIME = ZonedDateTime.parse("2025-01-01T00:00Z[UTC]");
    static final ZonedDateTime END_TIME =   ZonedDateTime.parse("2026-12-31T00:00Z[UTC]");

    CountBeneficiariesByCoveragePeriods query;

    @BeforeEach
    void setup() {
        query = new CountBeneficiariesByCoveragePeriods(container.getDataSource());
    }

    static ResultSet execute(String query) throws Exception {
        val c = container.getDataSource().getConnection();
        val s = c.createStatement();
        return s.executeQuery(query);
    }

    @Test
    void test_beneficiary_count_without_optout() {
        val periods = enumerateCoveragePeriods(START_TIME, END_TIME);
        assertEquals(3, query.countBeneficiaries("Z0000", periods, false));
        assertEquals(1, query.countBeneficiaries("Z7777", periods, false));
        assertEquals(1, query.countBeneficiaries("Z8888", periods, false));
        assertEquals(1, query.countBeneficiaries("Z9999", periods, false));
    }

    @Test
    void test_beneficiary_count_with_optout() {
        val periods = enumerateCoveragePeriods(START_TIME, END_TIME);
        assertEquals(1, query.countBeneficiaries("Z0000", periods, true));
        assertEquals(1, query.countBeneficiaries("Z7777", periods, true));
        assertEquals(0, query.countBeneficiaries("Z8888", periods, true));
        assertEquals(0, query.countBeneficiaries("Z9999", periods, true));
    }

    @Test
    void test_beneficiary_count_for_nonexistent_contract() {
        val periods = enumerateCoveragePeriods(START_TIME, END_TIME);
        assertEquals(0, query.countBeneficiaries("ABC", periods, false));
        assertEquals(0, query.countBeneficiaries("ABC", periods, true));
    }
}
