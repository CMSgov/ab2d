package gov.cms.ab2d.coverage.query;

import lombok.val;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

// Temporary utility for testing queries against dev DB
class TempTestUtils {

    static DataSource devDataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.postgresql.Driver");
        dataSourceBuilder.url(getenv("AB2D_DEV_DB_URL"));
        dataSourceBuilder.username(getenv("AB2D_DEV_DB_USER"));
        dataSourceBuilder.password(getenv("AB2D_DEV_DB_PASS"));
        return dataSourceBuilder.build();
    }

    static String getenv(String variable) {
        val result = System.getenv(variable);
        checkNotNull(result, "Environment variable not defined: " + variable);
        return result;
    }
}
