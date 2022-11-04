package gov.cms.ab2d.worker;

import gov.cms.ab2d.worker.bfdhealthcheck.BFDHealthCheckQuartzSetup;
import gov.cms.ab2d.worker.stuckjob.StuckJobQuartzSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.job",
        "gov.cms.ab2d.coverage",
        "gov.cms.ab2d.worker",
        "gov.cms.ab2d.bfd.client",
        "gov.cms.ab2d.audit",
        "gov.cms.ab2d.eventclient.clients",
        "gov.cms.ab2d.properties"
})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.job.model", "gov.cms.ab2d.coverage.model", "gov.cms.ab2d.properties.model"})
@EnableJpaRepositories(basePackages = {"gov.cms.ab2d.common.repository", "gov.cms.ab2d.job.repository",
        "gov.cms.ab2d.coverage.repository", "gov.cms.ab2d.properties.repository"})
@EnableRetry
@PropertySource("classpath:application.common.properties")
@Import({StuckJobQuartzSetup.class, BFDHealthCheckQuartzSetup.class})
public class SpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
