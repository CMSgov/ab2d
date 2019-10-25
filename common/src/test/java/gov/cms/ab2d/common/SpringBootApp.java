package gov.cms.ab2d.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "gov.cms.ab2d.common")
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
@EnableJpaRepositories(basePackages = "gov.cms.ab2d.common.repository")
public class SpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
