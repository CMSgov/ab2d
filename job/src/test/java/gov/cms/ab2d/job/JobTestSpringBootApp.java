package gov.cms.ab2d.job;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.job", "gov.cms.ab2d.common"})
@PropertySource("classpath:job-test.properties")
@Import(AB2DSQSMockConfig.class)
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.job.model", "gov.cms.ab2d.properties.model", "gov.cms.ab2d.contracts"})
@EnableFeignClients(clients = {ContractFeignClient.class})
public class JobTestSpringBootApp {
    public static void main(String [] args) {
        SpringApplication.run(JobTestSpringBootApp.class, args);
    }
}
