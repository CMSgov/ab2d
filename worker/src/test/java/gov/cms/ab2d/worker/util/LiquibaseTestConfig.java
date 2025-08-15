package gov.cms.ab2d.worker.util;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;


@TestConfiguration
public class LiquibaseTestConfig {

    @Bean(name = "testLiquibase")
    public SpringLiquibase testLiquibase(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password}") String pass
    ) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);                     // e.g., jdbc:postgresql://.../db?currentSchema=ab2d
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setMaximumPoolSize(1);
        SpringLiquibase lb = new SpringLiquibase();
        lb.setDataSource(ds);             // use direct URL instead of DataSource
        lb.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
        lb.setShouldRun(true);
        // lb.setContexts("test"); // if you use contexts
        return lb;
    }

    // Ensure EntityManagerFactory waits for Liquibase
    @Bean
    @DependsOn("testLiquibase")
    public BeanFactoryPostProcessor jpaAfterLiquibase() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                beanFactory.getBeanDefinition("entityManagerFactory")
                        .setDependsOn("testLiquibase");
            }
        };
    }
}
