package gov.cms.ab2d.hpms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.hpms",
})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
public class MockHpmsBoot {

    public static void main(String[] args) {
        SpringApplication.run(MockHpmsBoot.class, args);
    }
}
