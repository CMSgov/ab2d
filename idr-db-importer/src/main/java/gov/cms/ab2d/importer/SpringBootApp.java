package gov.cms.ab2d.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
@EnableRetry
public class SpringBootApp implements ApplicationRunner {

    private final CoverageV3S3Importer importer;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }


    @Override
    public void run(ApplicationArguments args) {
        log.info("IDR S3 import ECS task started");

        int exitCode = 1;
        try {
            importer.runOnce();
            exitCode = 0;
            log.info("ECS task completed successfully");
        } catch (Exception e) {
            log.error("IDR S3 import ECS task failed", e);
        } finally {
            System.exit(exitCode);
        }
    }
}

