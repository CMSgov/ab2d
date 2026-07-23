package gov.cms.ab2d.importer;

import datadog.trace.api.Trace;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.DatadogSpans;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

import static gov.cms.ab2d.common.util.PropertyConstants.V3_IDR_IMPORTER_STATUS;

@SpringBootApplication(scanBasePackages = {"gov.cms.ab2d.importer", "gov.cms.ab2d.common.properties"})
@EntityScan(basePackages = "gov.cms.ab2d.common.model")
@EnableJpaRepositories(basePackages = "gov.cms.ab2d.common.repository")
@Slf4j
@RequiredArgsConstructor
@EnableRetry
public class SpringBootApp implements ApplicationRunner {

    static final String STATUS_IN_PROGRESS = "import_in_progress";
    static final String STATUS_NOT_IN_PROGRESS = "import_not_in_progress";

    private final CoverageV3S3Importer importer;
    private final PropertiesService propertiesService;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }


    @Override
    @Trace(operationName = "ab2d.idr.import_task")
    public void run(ApplicationArguments args) {
        DatadogSpans.setTag("component", "idr");
        log.info("IDR S3 import ECS task started");
        updateStatus(STATUS_IN_PROGRESS);
        int exitCode = 1;
        try {
            importer.runOnce();
            exitCode = 0;
            log.info("IDR import ECS task completed successfully");
        } catch (Exception e) {
            DatadogSpans.markError(e);
            log.error("IDR import ECS task failed", e);
        } finally {
            updateStatus(STATUS_NOT_IN_PROGRESS);
            exit(exitCode);
        }
    }

    private void updateStatus(String status) {
        if (!propertiesService.updateProperty(V3_IDR_IMPORTER_STATUS, status)) {
            log.error("Failed to update IDR importer status to '{}'", status);
        }
    }

    protected void exit(int code) {
        System.exit(code);
    }
}

