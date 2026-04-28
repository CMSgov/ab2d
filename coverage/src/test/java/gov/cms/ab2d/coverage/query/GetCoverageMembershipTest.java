package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.filter.FilterOutByDate;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.assertEquals;

@Testcontainers
class GetCoverageMembershipTest {

    @Container
    private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

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

    @Test
    void test_Z8888_with_MBI_not_in_current_mbi_table() {
        // Note: Z8888 is the only contract where its single beneficiary is NOT in the `current_mbi` table
        val result = query.getCoverageMembership("Z8888", YEARS, true, DEFAULT_LIMIT);
        assertEquals("""
        patientId=5, year=2025, month=7
        patientId=5, year=2025, month=8
        patientId=5, year=2025, month=9
        """,
        toString(result));
    }

    @Test
    void testSummarizeMembership() {
        val contract = "Z0000";
        val contractDTO = new ContractForCoverageDTO();
        contractDTO.setContractNumber(contract);

        List<CoverageMembership> result = query.getCoverageMembership("Z0000", YEARS, false, DEFAULT_LIMIT);
        Map<Long, List<CoverageMembership>> map = new HashMap<>();
        for (CoverageMembership coverageMembership : result) {
            long patientId=coverageMembership.getIdentifiers().getPatientIdV3();
            map.putIfAbsent(patientId, new ArrayList<>());
            map.get(patientId).add(coverageMembership);
        }

        for (Map.Entry<Long, List<CoverageMembership>> entry : map.entrySet()) {
            val patientId = entry.getKey();
            val coverageMembrership = entry.getValue();
            //CoverageSummary coverageSummary = CoverageServiceRepository.summarizeCoverageMembership(contractDTO, entry);
            CoverageSummary coverageSummary = CoverageServiceRepository.summarizeCoverageMembership(contractDTO, entry);
            for (FilterOutByDate.DateRange dateRange : coverageSummary.getDateRanges()) {
                System.out.println(toString(dateRange));
            }
        }

    }

    private static String toString(FilterOutByDate.DateRange range) {
        return String.format("start=%s end=%s", range.getStart(), range.getEnd());
    }

    @Test
    void testDynamicTable() {

        val parameters = new MapSqlParameterSource();

        val template = new NamedParameterJdbcTemplate(container.getDataSource());

//        val query = "SELECT table_name FROM information_schema.tables";
        val query = "select * from v3.coverage_v3_Z0000";

        template.query(query, parameters, new RowMapper<Object>() {
            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                val contract = rs.getString(1);
                val patient_id = rs.getString(2);
                val current_mbi = rs.getString(3);

//                val result = rs.getString(1);
//                System.out.println(result);
                return null;
            }
        });

    }

    @Test
    void testAggregate() throws Exception {
        val years = List.of(2020,2021,2022,2023,2024,2025,2026);
        val contract = "Z0000";
        val contractDto = new ContractForCoverageDTO();
        contractDto.setContractNumber(contract);

        final String AGGREGATE_TEST_QUERY =
        """
            select patient_id, current_mbi, array_agg(array[year,month]) as coverage_dates
            from v3.coverage_v3_historical
            left join current_mbi on current_mbi = current_mbi.mbi
            where year in (:years)
                and contract = :contract
                and current_mbi is not null
                and share_data is not false
            group by patient_id, current_mbi
            order by patient_id, current_mbi
        """;

        val parameters = new MapSqlParameterSource()
                .addValue("contract", contract)
                .addValue("years", years);

        val template = new NamedParameterJdbcTemplate(container.getDataSource());

        val rowMapper = new RowMapper<Object>() {
            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                int patientId = rs.getInt(1);
                val currentMbi = rs.getString(2);
                val identifiers = Identifiers.ofV3(patientId, currentMbi);
                Array array = rs.getArray(3);
                Integer[][] intArray = (Integer[][]) array.getArray();
                List<CoverageMembership> membershipList = new ArrayList<>();
                for (Integer[] item : intArray) {
                    int year = item[0];
                    int month = item[1];
                    System.out.printf("contract=%s; patient=%s; year=%d; month=%d;%n", contract, patientId, year, month);
                    membershipList.add(new CoverageMembership(identifiers, year, month));
                }

                val coverageSummary = CoverageServiceRepository.summarizeCoverageMembership(contractDto, membershipList);


//                !FilterOutByDate.valid(resource, attTime, earliestDate, dateRanges);

                return coverageSummary;
            }
        };

        val result = template.query(AGGREGATE_TEST_QUERY, parameters, rowMapper);
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
}
