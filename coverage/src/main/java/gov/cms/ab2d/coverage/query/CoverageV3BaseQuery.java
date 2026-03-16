package gov.cms.ab2d.coverage.query;

import javax.sql.DataSource;

public abstract class CoverageV3BaseQuery {
    protected DataSource dataSource;

    protected CoverageV3BaseQuery(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
