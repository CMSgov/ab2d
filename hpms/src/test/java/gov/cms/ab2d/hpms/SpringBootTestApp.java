package gov.cms.ab2d.hpms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"gov.cms.ab2d.common", "gov.cms.ab2d.hpms", "gov.cms.ab2d.eventlogger", "gov.cms.ab2d.eventclient.clients"})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
@EnableJpaRepositories("gov.cms.ab2d.common.repository")
@PropertySource("classpath:application.common.properties")
public class SpringBootTestApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootTestApp.class, args);
    }
}
