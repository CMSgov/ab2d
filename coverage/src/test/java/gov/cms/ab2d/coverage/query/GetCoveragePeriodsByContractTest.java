package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.json.GsonTester;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import javax.sql.DataSource;
import java.util.Collections;

import static gov.cms.ab2d.coverage.query.TempTestUtils.devDataSource;

@Testcontainers
class GetCoveragePeriodsByContractTest {

    @Container
    private static final MyContainer container = new MyContainer();

    //@Test
    void _test() {
        val result = new GetCoveragePeriodsByContract(devDataSource()).getCoveragePeriodsForContract("Z0000");
        System.out.println(result);
    }

    @Test
    void test() throws Exception {
        DataSource dataSource;
        while ((dataSource = container.dataSource) == null) {
            System.out.println("Waiting...");
            Thread.sleep(1000);
        }
        System.out.println("DataSource is ready");

        System.out.println(new GetCoveragePeriodsByContract(dataSource).getCoveragePeriodsForContract("Z1234"));
    }

    public static class MyContainer extends PostgreSQLContainer<MyContainer> {

        DataSource dataSource;

        public MyContainer() {
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
            dataSource = dataSourceBuilder.build();

        }
    }

}
