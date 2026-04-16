package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.model.v3.CoverageV3;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Count;
import lombok.val;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetCoverageV3Count extends CoverageV3BaseQuery {

	private static final String QUERY =
	"""
	SELECT contract, year, month, COUNT(*)
	FROM v3.coverage_v3
	GROUP BY contract, year, month
	ORDER BY contract, month desc, year desc
	""";

	public GetCoverageV3Count(DataSource dataSource) {
		super(dataSource);
	}

	public Map<String, List<CoverageV3Count>> coverageCounts() {
		val template = new JdbcTemplate(this.dataSource);
		List<CoverageV3Count> queryResult = template.query(QUERY, new CoverageV3CountRowMapper());
		Map<String, List<CoverageV3Count>> map = new HashMap<>();

		for (CoverageV3Count coverageV3Count : queryResult) {
			val contract = coverageV3Count.getContractNumber();
			var list = map.get(contract);
			if (list == null) {
				list = new ArrayList<>();
				map.put(contract, list);
			}
			list.add(coverageV3Count);
		}

		return map;
	}

	private static class CoverageV3CountRowMapper implements RowMapper<CoverageV3Count> {
		@Override
		public CoverageV3Count mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new CoverageV3Count(
				rs.getString(1),
				rs.getInt(2),
				rs.getInt(3),
				rs.getInt(4)
			);
		}
	}


}
