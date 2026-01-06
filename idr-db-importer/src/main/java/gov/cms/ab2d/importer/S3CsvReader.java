package gov.cms.ab2d.importer;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class S3CsvReader {
    private static final Logger log = LoggerFactory.getLogger(S3CsvReader.class);

    @Value("${app.awsRegion}")
    private String awsRegion;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.key}")
    private String key;

    private S3Client s3;

    @javax.annotation.PostConstruct
    void init() {
        this.s3 = S3Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }
//
//    public void readAndPreview() throws Exception {
//        var head = s3.headObject(HeadObjectRequest.builder()
//                .bucket(bucket)
//                .key(key)
//                .build());
//
//        log.info("S3 object found. bucket={} key={} size={} etag={}",
//                bucket, key, head.contentLength(), head.eTag());
//
//        GetObjectRequest req = GetObjectRequest.builder()
//                .bucket(bucket)
//                .key(key)
//                .build();
//
//        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(req);
//             CSVReader reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
//
//            String[] header = reader.readNext();
//            if (header == null) {
//                throw new IllegalStateException("CSV file is empty");
//            }
//
//            log.info("CSV header: {}", Arrays.toString(header));
//
//            for (int i = 1; i <= previewRows; i++) {
//                String[] row = reader.readNext();
//                if (row == null) {
//                    break;
//                }
//                log.info("CSV row {}: {}", i, Arrays.toString(row));
//            }
//        }
//    }
}

