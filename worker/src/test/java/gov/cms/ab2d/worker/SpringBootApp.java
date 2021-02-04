package gov.cms.ab2d.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.worker",
        "gov.cms.ab2d.eventlogger",
        "gov.cms.ab2d.bfd.client"
})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
@EnableJpaRepositories("gov.cms.ab2d.common.repository")
@PropertySource("classpath:application.common.properties")
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

}
