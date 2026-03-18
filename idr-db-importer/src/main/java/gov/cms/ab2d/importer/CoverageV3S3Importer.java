package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class CoverageV3S3Importer {

    @Value("${app.awsRegion:us-east-1}")
    private Region region;

    @Value("${app.s3.bucket}")
    private String bucket;


    private final CoverageV3ImportService importService;

     public CoverageV3S3Importer(CoverageV3ImportService importService) {
        this.importService = importService;
    }

    public void runOnce() throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String finalKey = "coverage_v3_" + date + ".csv";
        String fqtn = "v3.coverage_v3";
        log.info("Starting import of s3://{}/{} into {}", bucket, finalKey, fqtn);
        importService.importWithRetry(fqtn, bucket, finalKey, region.id());
    }

}
