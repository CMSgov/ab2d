package gov.cms.ab2d.e2etest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:application.e2e.properties")
public class SpringBootApp {
    public static void main(String [] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
