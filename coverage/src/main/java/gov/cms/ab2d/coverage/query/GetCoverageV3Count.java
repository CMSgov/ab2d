package gov.cms.ab2d.coverage.query;

import javax.sql.DataSource;

public class GetCoverageV3Count extends CoverageV3BaseQuery {

	private static final String QUERY =
	"""
	SELECT contract, year, month, COUNT(*)
	FROM v3.coverage_v3
	GROUP BY contract, year, month
	HAVING COUNT(*) > 1
	ORDER BY contract, month desc, year desc
	""";

	public GetCoverageV3Count(DataSource dataSource) {
		super(dataSource);
	}
}
