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
    void test_Z0000_without_optout() {
        val result = query.getCoverageMembership("Z0000", YEARS, false, DEFAULT_LIMIT);
        assertEquals(18, result.size());

        assertEquals(toString(result),
        """
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
        """);
    }

    @Test
    void test_Z0000_with_optout() {
        val result = query.getCoverageMembership("Z0000", YEARS, true, DEFAULT_LIMIT);

        assertEquals(toString(result),
        """
        patientId=2, year=2026, month=2
        patientId=2, year=2026, month=1
        patientId=2, year=2025, month=12
        patientId=2, year=2025, month=11
        patientId=2, year=2025, month=10
        patientId=2, year=2025, month=9
        """);
    }

    @Test
    void test_Z0000_with_() {

        val result = query.getCoverageMembership("Z0000", YEARS, false, 5);

        System.out.println();

        val result2 = query.getCoverageMembership("Z0000", YEARS, false, 5, 2L);

        System.out.println();


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

    //@Test
    void _test() {
        val years = CoverageServiceRepository.YEARS;
        val pageLimit=1000;
        val optOutOn=false;
        val contract="Z0000";
        boolean done = false;

        Long cursor = null;
        while (!done) {
            val result = new GetCoverageMembership(devDataSource())
                    .getCoverageMembership(contract, years, optOutOn, 1000, cursor);

            if (result.size() < pageLimit) {
                done=true;
            }
            else {
                cursor = result.get(result.size()-1).getIdentifiers().getPatientIdV3();
            }

            System.out.println(result);
        }
    }
}
