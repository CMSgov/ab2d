package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.model.Identifiers;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;

public class GetCoverageMembership extends CoverageV3BaseQuery {

    private final CoverageMembershipRowMapper mapper;
    public GetCoverageMembership(DataSource dataSource) {
        super(dataSource);
        this.mapper = new CoverageMembershipRowMapper();
    }

    private static final String SELECT_COVERAGE_WITHOUT_OPTOUT_WITHOUT_CURSOR =
    """
    select patient_id, current_mbi, historic_mbis, year, month from (
        select * from v3.coverage_v3
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
        union
        select * from v3.coverage_v3_historical
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
        order by patient_id, year asc, month asc
        limit :limit
    )
    """;

    private static final String SELECT_COVERAGE_WITHOUT_OPTOUT_WITH_CURSOR =
    """
    select patient_id, current_mbi, historic_mbis, year, month from (
        select * from v3.coverage_v3
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
           and patient_id >= :patient_id
        union
        select * from v3.coverage_v3_historical
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
           and patient_id >= :patient_id
        order by patient_id, year asc, month asc
        limit :limit
    )
    """;

    private static final String SELECT_COVERAGE_WITH_OPTOUT_WITHOUT_CURSOR =
    """
    select patient_id, current_mbi, historic_mbis, year, month from
    (
       select * from v3.coverage_v3
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
       union
       select * from  v3.coverage_v3_historical
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
    ) as union_results
    join current_mbi on union_results.current_mbi = current_mbi.mbi
    where current_mbi is not null
    and share_data is not false
    limit :limit
    """;

    private static final String SELECT_COVERAGE_WITH_OPTOUT_WITH_CURSOR =
    """
    select patient_id, current_mbi, historic_mbis, year, month from
    (
       select * from v3.coverage_v3
           where contract = :contract
           and year in (:years)
           and current_mbi is not null
           and patient_id >= :patient_id
       union
       select * from  v3.coverage_v3_historical
           where contract = :contract
           and current_mbi is not null
           and year in (:years)
           and patient_id >= :patient_id
    ) as union_results
    join current_mbi on union_results.current_mbi = current_mbi.mbi
    where current_mbi is not null
    and share_data is not false
    limit :limit
    """;

    public List<CoverageMembership> getCoverageMembership(
            final String contract,
            final List<Integer> years,
            final boolean optOutOn,
            final long limit
    ) {
        return getCoverageMembership(contract, years, optOutOn, limit, null);
    }

    public List<CoverageMembership> getCoverageMembership(
            final String contract,
            final List<Integer> years,
            final boolean optOutOn,
            final long limit,
            final Long cursorPatientId
    ) {

        val hasCursor = cursorPatientId != null;
        final String query;
        if (optOutOn) {
            if (hasCursor) {
                query = SELECT_COVERAGE_WITH_OPTOUT_WITH_CURSOR;
            } else {
                query = SELECT_COVERAGE_WITH_OPTOUT_WITHOUT_CURSOR;
            }
        } else {
            if (hasCursor) {
                query = SELECT_COVERAGE_WITHOUT_OPTOUT_WITH_CURSOR;
            } else {
                query = SELECT_COVERAGE_WITHOUT_OPTOUT_WITHOUT_CURSOR;
            }
        }

        val parameters = new MapSqlParameterSource()
                .addValue("contract", contract)
                .addValue("years", years)
                .addValue("limit", limit);

        if (hasCursor) {
            parameters.addValue("patient_id", cursorPatientId);
        }

        val template = new NamedParameterJdbcTemplate(this.dataSource);
        return template.query(query, parameters, this.mapper);
    }

    private static class CoverageMembershipRowMapper implements RowMapper<CoverageMembership> {

        @Override
        public CoverageMembership mapRow(ResultSet rs, int rowNum) throws SQLException {
            val patientId = rs.getInt(1);
            val currentMbi = rs.getString(2);
            val historicMbis = historicMbisToSet(rs.getString(3));
            val year = rs.getInt(4);
            val month = rs.getInt(5);
            val identifiers = Identifiers.ofV3(patientId, currentMbi, historicMbis);
            return new CoverageMembership(identifiers, year, month);
        }

        private LinkedHashSet<String> historicMbisToSet(String value) {
            if (StringUtils.isNotBlank(value) && !value.equalsIgnoreCase("NULL")) {
                val mbis = value.split(",");
                val result = new LinkedHashSet<String>(mbis.length);
                for (String mbi : mbis) {
                    result.add(mbi);
                }
                return result;
            }
            return new LinkedHashSet<>(0);
        }
    }
}
