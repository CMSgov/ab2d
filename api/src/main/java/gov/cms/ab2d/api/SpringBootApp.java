package gov.cms.ab2d.api;

import gov.cms.ab2d.api.config.MDCFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.common", "gov.cms.ab2d.api", "gov.cms.ab2d.hpms"})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
@EnableJpaRepositories("gov.cms.ab2d.common.repository")
@PropertySource("classpath:application.common.properties")
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

    @Bean
    public FilterRegistrationBean registerRequestLogFilter(MDCFilter filter) {
        FilterRegistrationBean reg = new FilterRegistrationBean(filter);
        // Spring security is -100, so run before it
        reg.setOrder(-101);
        return reg;
    }
}
