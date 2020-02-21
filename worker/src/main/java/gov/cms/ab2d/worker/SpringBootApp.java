package gov.cms.ab2d.worker;

import com.google.common.collect.Maps;
import gov.cms.ab2d.optout.setup.OptOutQuartzSetup;
import gov.cms.ab2d.worker.stuckjob.StuckJobQuartzSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

import java.util.Map;


@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.worker",
        "gov.cms.ab2d.bfd.client",
        "gov.cms.ab2d.audit",
        "gov.cms.ab2d.optout"
})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
@EnableJpaRepositories("gov.cms.ab2d.common.repository")
@EnableRetry
@PropertySource("classpath:application.common.properties")
@Import({OptOutQuartzSetup.class, StuckJobQuartzSetup.class})
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SpringBootApp.class);
        springApplication.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            ConfigurableEnvironment env = event.getEnvironment();
            Map<String, Object> properties = Maps.newHashMap();
            properties.put("pcp.core.pool.size", 10);
            properties.put("pcp.max.pool.size", 150);
            properties.put("pcp.scaleToMax.time", 900);

            env.getPropertySources().addLast(new MapPropertySource("db", properties));
        });
        springApplication.run(args);
    }

}
