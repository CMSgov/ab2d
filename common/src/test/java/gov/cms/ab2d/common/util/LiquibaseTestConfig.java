package gov.cms.ab2d.common.util;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import java.sql.SQLException;


@TestConfiguration
public class LiquibaseTestConfig {

    @Bean(name = "testLiquibase")
    public SpringLiquibase testLiquibase(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password}") String pass
    ) throws SQLException {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setMaximumPoolSize(1);
        SpringLiquibase lb = new SpringLiquibase();
        lb.setDataSource(ds);
        lb.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
        lb.setShouldRun(true);
        try (var c = ds.getConnection(); var s = c.createStatement(); var rs = s.executeQuery("select current_schema")) {
            rs.next();
            System.out.println("current_schema = " + rs.getString(1)); // should print "ab2d"
        }
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
