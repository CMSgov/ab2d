package gov.cms.ab2d.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:application.common.properties")
public class SpringBootApp {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
