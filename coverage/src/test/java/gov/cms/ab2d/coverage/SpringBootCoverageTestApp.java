package gov.cms.ab2d.coverage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.coverage",
        "gov.cms.ab2d.eventlogger"
})
//@PropertySource("classpath:application.common.properties")
public class SpringBootCoverageTestApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootCoverageTestApp.class, args);
    }
}
