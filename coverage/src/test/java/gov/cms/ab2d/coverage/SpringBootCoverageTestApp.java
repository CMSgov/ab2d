package gov.cms.ab2d.coverage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.coverage",
        "gov.cms.ab2d.eventlogger"
})
@PropertySource("classpath:application.common.properties")
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.coverage.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.coverage.repository"})
public class SpringBootCoverageTestApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootCoverageTestApp.class, args);
    }
}
