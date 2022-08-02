package gov.cms.ab2d.common;

import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.eventlogger", "gov.cms.ab2d.common", "gov.cms.ab2d.eventclient.clients"})
@PropertySource("classpath:application.common.properties")
@Import(AB2DSQSMockConfig.class)
public class SpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
