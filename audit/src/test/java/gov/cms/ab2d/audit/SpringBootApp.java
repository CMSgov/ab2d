package gov.cms.ab2d.audit;

import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.common", "gov.cms.ab2d.job", "gov.cms.ab2d.audit", "gov.cms.ab2d.eventclient.clients"})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.job.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.job.repository"})
@PropertySource("classpath:application.common.properties")
@Import(AB2DSQSMockConfig.class)
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
