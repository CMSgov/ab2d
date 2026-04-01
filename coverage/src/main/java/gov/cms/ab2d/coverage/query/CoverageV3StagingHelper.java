package gov.cms.ab2d.coverage.query;

import lombok.val;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

public class CoverageV3StagingHelper extends CoverageV3BaseQuery {

    public CoverageV3StagingHelper(DataSource dataSource) {
        super(dataSource);
    }

    private static final String COUNT_FOR_COVERAGE_V3 =
        "select count(*) from v3.coverage_v3_copy where contract = :contract";

    private static final String COUNT_FOR_COVERAGE_V3_STAGING =
        "select count(*) from v3.coverage_v3_staging_copy where contract = :contract";

    private static final String DELETE_FROM_COVERAGE_AND_GET_ROWS_DELETED =
    """
    with deleted_rows as (
        delete from v3.coverage_v3_copy where contract = :contract returning *
    )
    select count(*) from deleted_rows
    """;


    private static final String DELETE_FROM_STAGING_AND_GET_ROWS_DELETED =
    """
    with deleted_rows as (
        delete from v3.coverage_v3_staging_copy where contract = :contract returning *
    )
    select count(*) from deleted_rows
    """;


    private static final String COPY_FROM_STAGING_TO_COVERAGE_V3 =
    """
    with inserted_rows as (
        insert into v3.coverage_v3_copy select * from v3.coverage_v3_staging_copy where contract = :contract returning *
    )
    select count(*) from inserted_rows;
    """;

    public int getCoveragePeriodCountForCoverageV3(final String contract) {
        return executeQuery(contract, COUNT_FOR_COVERAGE_V3);
    }

    public int getCoveragePeriodCountForCoverageV3Staging(final String contract) {
        return executeQuery(contract, COUNT_FOR_COVERAGE_V3_STAGING);
    }

    public int copyFromStagingToCoverage(final String contract) {
        return executeQuery(contract, COPY_FROM_STAGING_TO_COVERAGE_V3);
    }

    public int deleteFromCoverageAndGetRowsDeleted(String contract) {
        return executeQuery(contract, DELETE_FROM_COVERAGE_AND_GET_ROWS_DELETED);
    }

    public int deleteFromStagingAndGetRowsDeleted(String contract) {
        return executeQuery(contract, DELETE_FROM_STAGING_AND_GET_ROWS_DELETED);
    }

    private int executeQuery(final String contract, final String query) {
        val parameters = new MapSqlParameterSource().addValue("contract", contract);
        val template = new NamedParameterJdbcTemplate(this.dataSource);
        return DataAccessUtils.intResult(template.queryForList(query, parameters, Integer.class));
    }

}
