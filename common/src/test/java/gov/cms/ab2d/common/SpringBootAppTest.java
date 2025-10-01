package gov.cms.ab2d.common;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SuppressWarnings("java:S2187") // Not a test class; entrypoint only
@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.common", "gov.cms.ab2d.eventclient.clients", "gov.cms.ab2d.contracts"})
@PropertySource("classpath:application.common.properties")
@Import(AB2DSQSMockConfig.class)
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.properties.model", "gov.cms.ab2d.contracts"})
@EnableFeignClients(clients = {ContractFeignClient.class})
public class SpringBootAppTest {

    public static void main(String [] args) {
        SpringApplication.run(SpringBootAppTest.class, args);
    }
}
