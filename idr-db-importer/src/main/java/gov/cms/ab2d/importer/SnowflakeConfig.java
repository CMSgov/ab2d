package gov.cms.ab2d.importer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Bean
    @ConditionalOnProperty(name = "app.snowflake.url")
    public SnowflakeCoverageQueryService snowflakeCoverageQueryService(
            @Value("${app.snowflake.url}") String url,
            @Value("${app.snowflake.user}") String user,
            @Value("${app.snowflake.privateKey}") String privateKeyPem,
            @Value("${app.snowflake.role}") String role,
            @Value("${app.snowflake.warehouse}") String warehouse,
            @Value("${app.snowflake.db}") String db,
            @Value("${app.snowflake.schema}") String schema
    ) {
        return new SnowflakeCoverageQueryService(
                url,
                user,
                privateKeyPem,
                role,
                warehouse,
                db,
                schema
        );
    }
}
