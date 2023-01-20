package gov.cms.ab2d.coverage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.coverage",
        "gov.cms.ab2d.eventclient.clients"
})
@PropertySource("classpath:application.coverage.properties")
@EntityScan(basePackages = {"gov.cms.ab2d.contracts"})
public class SpringBootCoverageTestApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootCoverageTestApp.class, args);
    }
}
