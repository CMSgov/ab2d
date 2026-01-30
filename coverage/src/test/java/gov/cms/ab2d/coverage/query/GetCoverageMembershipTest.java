package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.coverage.util.Coverage;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static gov.cms.ab2d.coverage.query.TempTestUtils.devDataSource;
import static org.junit.Assert.assertEquals;

@Testcontainers
class GetCoverageMembershipTest {

    @Container
    private static final PostgresTestContainer container = new PostgresTestContainer();

    static final List<Integer> YEARS = Arrays.asList(2025,2026);

    static final int DEFAULT_LIMIT = 1000;

    GetCoverageMembership query;

    @BeforeEach
    void setup() {
        query = new GetCoverageMembership(container.getDataSource());
    }

    @Test
    void test_Z0000_without_optout_without_cursor() {
        val result = query.getCoverageMembership("Z0000", YEARS, false, DEFAULT_LIMIT);
        assertEquals(18, result.size());

        assertEquals("""
        patientId=1, year=2025, month=6
        patientId=1, year=2025, month=7
        patientId=1, year=2025, month=8
        patientId=1, year=2025, month=9
        patientId=1, year=2025, month=10
        patientId=1, year=2025, month=11
        patientId=1, year=2025, month=12
        patientId=1, year=2026, month=1
        patientId=1, year=2026, month=2
        patientId=2, year=2025, month=9
        patientId=2, year=2025, month=10
        patientId=2, year=2025, month=11
        patientId=2, year=2025, month=12
        patientId=2, year=2026, month=1
        patientId=2, year=2026, month=2
        patientId=3, year=2025, month=12
        patientId=3, year=2026, month=1
        patientId=3, year=2026, month=2
        """,
        toString(result));
    }

    @Test
    void test_Z0000_with_optout_without_cursor() {
        val result = query.getCoverageMembership("Z0000", YEARS, true, DEFAULT_LIMIT);
        assertEquals("""
        patientId=2, year=2025, month=9
        patientId=2, year=2025, month=10
        patientId=2, year=2025, month=11
        patientId=2, year=2025, month=12
        patientId=2, year=2026, month=1
        patientId=2, year=2026, month=2
        """,
        toString(result));
    }

    @Test
    void test_Z0000_without_optout_and_with_cursor() {
        var result = query.getCoverageMembership("Z0000", YEARS, false, 5);
        assertEquals(5, result.size());
        assertEquals("""
        patientId=1, year=2025, month=6
        patientId=1, year=2025, month=7
        patientId=1, year=2025, month=8
        patientId=1, year=2025, month=9
        patientId=1, year=2025, month=10
        """,
        toString(result));

        // set cursor parent ID to 2L
        result = query.getCoverageMembership("Z0000", YEARS, false, 5, 2L);
        assertEquals(5, result.size());
        assertEquals("""
        patientId=2, year=2025, month=9
        patientId=2, year=2025, month=10
        patientId=2, year=2025, month=11
        patientId=2, year=2025, month=12
        patientId=2, year=2026, month=1
        """,
        toString(result));

        // set cursor parent ID to 3L
        result = query.getCoverageMembership("Z0000", YEARS, false, 5, 3L);
        assertEquals(3, result.size());
        assertEquals("""
        patientId=3, year=2025, month=12
        patientId=3, year=2026, month=1
        patientId=3, year=2026, month=2
        """,
        toString(result));
    }

    @Test
    void test_Z0000_with_optout_with_and_with_cursor() {
        var result = query.getCoverageMembership("Z0000", YEARS, true, 5);
        assertEquals(5, result.size());
        assertEquals("""
        patientId=2, year=2025, month=9
        patientId=2, year=2025, month=10
        patientId=2, year=2025, month=11
        patientId=2, year=2025, month=12
        patientId=2, year=2026, month=1
        """,
        toString(result));

        // explicitly set cursor patient ID to 2
        result = query.getCoverageMembership("Z0000", YEARS, true, 5, 2L);
        assertEquals(5, result.size());
        assertEquals("""
        patientId=2, year=2025, month=9
        patientId=2, year=2025, month=10
        patientId=2, year=2025, month=11
        patientId=2, year=2025, month=12
        patientId=2, year=2026, month=1
        """,
        toString(result));
    }

    @Test
    void test_nonexistent_contract() {
        var result = query.getCoverageMembership("ABC", YEARS, false, DEFAULT_LIMIT);
        assertEquals(0, result.size());
        result = query.getCoverageMembership("ABC", YEARS, true, DEFAULT_LIMIT);
        assertEquals(0, result.size());
    }

    @Test
    void test_Z0000_with_invalid_cursor() {
        val result = query.getCoverageMembership("Z0000", YEARS, false, 5, 4L);
        assertEquals(0, result.size());
    }

    @Test
    void test_Z0000_with_2026_only() {
        val result = query.getCoverageMembership("Z0000", List.of(2026), false, DEFAULT_LIMIT);
        assertEquals("""
        patientId=1, year=2026, month=1
        patientId=1, year=2026, month=2
        patientId=2, year=2026, month=1
        patientId=2, year=2026, month=2
        patientId=3, year=2026, month=1
        patientId=3, year=2026, month=2
        """,
        toString(result));
    }

    @Test
    void test_Z0000_with_nonexistent_years() {
        val result = query.getCoverageMembership("Z0000", List.of(2020), false, DEFAULT_LIMIT);
        assertEquals(0, result.size());
    }

    String toString(List<CoverageMembership> list) {
        val sb = new StringBuilder();
        list.forEach(item -> sb.append(toString(item)).append("\n"));
        return sb.toString();
    }

    String toString(CoverageMembership membership) {
        return String.format(
            "patientId=%s, year=%d, month=%d",
            membership.getIdentifiers().getPatientIdV3(),
            membership.getYear(),
            membership.getMonth()
        );
    }
}
