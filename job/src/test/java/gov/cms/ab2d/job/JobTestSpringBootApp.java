package gov.cms.ab2d.job;

import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.job", "gov.cms.ab2d.common"})
@PropertySource("classpath:job-test.properties")
@EntityScan(basePackages = {"gov.cms.ab2d.job.model", "gov.cms.ab2d.common.model"})
@Import(AB2DSQSMockConfig.class)
public class JobTestSpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(JobTestSpringBootApp.class, args);
    }
}
