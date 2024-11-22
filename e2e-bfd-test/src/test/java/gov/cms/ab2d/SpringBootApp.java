package gov.cms.ab2d;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.bfd.client",
        "gov.cms.ab2d.eventclient.clients",
        "gov.cms.ab2d.snsclient.clients",
        "gov.cms.ab2d.common.config",
        "gov.cms.ab2d.common.feign",
        "gov.cms.ab2d.common.model",
        "gov.cms.ab2d.common.properties",
        "gov.cms.ab2d.common.repository",
        "gov.cms.ab2d.common.service",
        "gov.cms.ab2d.coverage.repository",
        "gov.cms.ab2d.coverage.service",
        "gov.cms.ab2d.job.service",
        "gov.cms.ab2d.worker.config",
        "gov.cms.ab2d.worker.processor",
        "gov.cms.ab2d.worker.service"
})
@EnableJpaRepositories(basePackages = {"gov.cms.ab2d.common.repository", "gov.cms.ab2d.job.repository",
        "gov.cms.ab2d.coverage.repository"})
@EnableFeignClients(clients = {ContractFeignClient.class})
@EnableRetry
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
