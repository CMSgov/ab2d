package gov.cms.ab2d.coverage.query;

import lombok.Getter;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Collections;

@Getter
class PostgresTestContainer extends PostgreSQLContainer<PostgresTestContainer> {

    private DataSource dataSource;

    PostgresTestContainer() {
        super("postgres:16");
    }

    @Override
    public void start() {
        super.withUsername("cmsadmin");
        super.withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        super.withInitScript("v3/init.sql");
        super.start();

        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.postgresql.Driver");
        dataSourceBuilder.url(getJdbcUrl());
        dataSourceBuilder.username(getUsername());
        dataSourceBuilder.password(getPassword());
        this.dataSource = dataSourceBuilder.build();
    }

}
