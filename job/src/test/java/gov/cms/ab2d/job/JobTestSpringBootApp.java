package gov.cms.ab2d.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.eventlogger", "gov.cms.ab2d.job", "gov.cms.ab2d.common"})
@PropertySource("classpath:job-test.properties")
public class JobTestSpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(JobTestSpringBootApp.class, args);
    }
}
