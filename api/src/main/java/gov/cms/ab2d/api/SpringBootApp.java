package gov.cms.ab2d.api;

import gov.cms.ab2d.api.config.MDCFilter;
import gov.cms.ab2d.hpms.quartz.HPMSIngestQuartzSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.common", "gov.cms.ab2d.job", "gov.cms.ab2d.api",
        "gov.cms.ab2d.hpms", "gov.cms.ab2d.eventlogger", "gov.cms.ab2d.eventclient.clients"})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.job.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.job.repository"})
@PropertySource("classpath:application.common.properties")
@Import({HPMSIngestQuartzSetup.class})
public class SpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
    @Bean
    public FilterRegistrationBean<MDCFilter> registerRequestLogFilter(MDCFilter filter) {
        FilterRegistrationBean<MDCFilter> reg = new FilterRegistrationBean<>(filter);
        // Spring security is -100, so run before it
        reg.setOrder(-101);
        return reg;
    }
}
