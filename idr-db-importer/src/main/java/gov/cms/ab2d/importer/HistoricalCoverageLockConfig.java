package gov.cms.ab2d.importer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import javax.sql.DataSource;

@Configuration
public class HistoricalCoverageLockConfig {

    @Bean
    public JdbcLockRegistry historicalCoverageLockRegistry(ApplicationContext context, DataSource dataSource) {
        DefaultLockRepository repository = new DefaultLockRepository(dataSource);
        repository.setApplicationContext(context);
        repository.afterSingletonsInstantiated();
        repository.afterPropertiesSet();
        return new JdbcLockRegistry(repository);
    }
}
