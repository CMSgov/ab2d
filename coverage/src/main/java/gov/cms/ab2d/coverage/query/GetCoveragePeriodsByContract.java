package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.val;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class GetCoveragePeriodsByContract extends CoverageV3BaseQuery {

    protected final YearMonthRecordRowMapper mapper;
    public GetCoveragePeriodsByContract(DataSource dataSource) {
        super(dataSource);
        this.mapper = new YearMonthRecordRowMapper();
    }

    private static final String SELECT_COVERAGE_PERIODS  =
    """
    select distinct year, month from v3.coverage_v3 where contract = :contract
    union
    select distinct year, month from v3.coverage_v3_historical where contract = :contract
    order by year asc, month asc
    """;

    public List<YearMonthRecord> getCoveragePeriodsForContract(final String contract) {
        val parameters = new MapSqlParameterSource().addValue("contract", contract);
        val template = new NamedParameterJdbcTemplate(this.dataSource);
        return template.query(SELECT_COVERAGE_PERIODS, parameters, this.mapper);
    }

    private static class YearMonthRecordRowMapper implements RowMapper<YearMonthRecord> {
        @Override
        public YearMonthRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new YearMonthRecord(rs.getInt(1), rs.getInt(2));
        }
    }
}
