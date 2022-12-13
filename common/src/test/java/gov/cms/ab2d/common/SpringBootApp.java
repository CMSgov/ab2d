package gov.cms.ab2d.common;

import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.common", "gov.cms.ab2d.eventclient.clients", "gov.cms.ab2d.contracts"})
@PropertySource("classpath:application.common.properties")
@Import(AB2DSQSMockConfig.class)
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.job.model", "gov.cms.ab2d.properties.model", "gov.cms.ab2d.contracts"})
public class SpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
