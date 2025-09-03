package gov.cms.ab2d.eventclient;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.eventclient"})
public class SpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

    @Bean
    public Ab2dEnvironment getEnvironment() {
        return Ab2dEnvironment.fromName("local");
    }
}

