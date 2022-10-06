package gov.cms.ab2d.job;

import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.eventlogger", "gov.cms.ab2d.job", "gov.cms.ab2d.common"})
@PropertySource("classpath:job-test.properties")
@Import(AB2DSQSMockConfig.class)
public class JobTestSpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(JobTestSpringBootApp.class, args);
    }
}
