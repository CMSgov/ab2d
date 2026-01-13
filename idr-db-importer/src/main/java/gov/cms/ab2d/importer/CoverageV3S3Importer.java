package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class CoverageV3S3Importer {

    @Value("${app.awsRegion:us-east-1}")
    private Region region;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${coverage.import.schema}")
    private String schema;

    @Value("${coverage.import.table}")
    private String table;

    private final S3Client s3Client;

    private final CoverageV3ImportService importService;

    public CoverageV3S3Importer(S3Client s3Client, CoverageV3ImportService importService) {
        this.s3Client = s3Client;
        this.importService = importService;
    }

    public void runOnce() throws Exception {
        String fileKey = findTodayDatedCsv(s3Client);
        String fqtn = schema + "." + table;

        log.info("Starting import of s3://{}/{} into {}", bucket, fileKey, fqtn);
        importService.importWithRetry(fqtn, bucket, fileKey, region.id());
    }

    private String findTodayDatedCsv(S3Client s3) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMddyy"));

        ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);

        List<String> matches = s3.listObjectsV2(req.build()).contents().stream()
                .map(S3Object::key)
                .filter(k -> !k.endsWith("/"))
                .filter(k -> k.toLowerCase().endsWith(".csv"))
                .filter(k -> k.contains(today))
                .toList();

        if (matches.size() != 1) {
            throw new IllegalStateException("Expected exactly one CSV containing " + today + " but found " + matches);
        }
        return matches.get(0);
    }

}
