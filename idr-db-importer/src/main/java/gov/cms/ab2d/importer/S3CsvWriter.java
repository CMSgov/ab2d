package gov.cms.ab2d.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3CsvWriter {

    private final S3Client s3;

    private static final int PART_BYTES = 8 * 1024 * 1024; // 8MB

    public void writeSnowflakeToS3(String bucket, String finalKey, ResultSet rs) throws Exception {
        String tmpKey = finalKey + ".tmp";
        String uploadId = s3.createMultipartUpload(r -> r.bucket(bucket).key(tmpKey).contentType("text/csv")).uploadId();

        List<CompletedPart> parts = new ArrayList<>();
        int partNum = 1;

        ByteArrayOutputStream buf = new ByteArrayOutputStream(PART_BYTES);

        // Header must align with COPY column list in CoverageV3ImportService
        writeLine(buf, "patient_id,contract,year,month,current_mbi,historic_mbis");

        try {
            while (rs.next()) {
                String line = String.join(",",
                        csvLong(rs.getLong("patient_id")),
                        csvStr(rs.getString("contract")),
                        String.valueOf(rs.getInt("year")),
                        String.valueOf(rs.getInt("month")),
                        csvStr(rs.getString("current_mbi")),
                        csvNullableStr(rs.getString("historic_mbis"))
                );
                writeLine(buf, line);

                if (buf.size() >= PART_BYTES) {
                    parts.add(uploadPart(bucket, tmpKey, uploadId, partNum++, buf.toByteArray()));
                    buf.reset();
                }
            }

            if (buf.size() > 0) {
                parts.add(uploadPart(bucket, tmpKey, uploadId, partNum, buf.toByteArray()));
            }

            s3.completeMultipartUpload(r -> r.bucket(bucket).key(tmpKey).uploadId(uploadId)
                    .multipartUpload(m -> m.parts(parts)));

            // Publish final
            s3.copyObject(r -> r.sourceBucket(bucket).sourceKey(tmpKey).destinationBucket(bucket).destinationKey(finalKey));
            s3.deleteObject(r -> r.bucket(bucket).key(tmpKey));

        } catch (Exception e) {
            log.warn("Aborting multipart upload uploadId={} for s3://{}/{}", uploadId, bucket, tmpKey, e);
            s3.abortMultipartUpload(r -> r.bucket(bucket).key(tmpKey).uploadId(uploadId));
            throw e;
        }
    }

    private CompletedPart uploadPart(String bucket, String key, String uploadId, int partNumber, byte[] bytes) {
        UploadPartResponse resp = s3.uploadPart(
                r -> r.bucket(bucket).key(key).uploadId(uploadId).partNumber(partNumber).contentLength((long) bytes.length),
                RequestBody.fromBytes(bytes)
        );
        return CompletedPart.builder().partNumber(partNumber).eTag(resp.eTag()).build();
    }

    private static void writeLine(ByteArrayOutputStream out, String line) throws Exception {
        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.write('\n');
    }

    private static String csvStr(String v) {
        if (v == null) return "NULL";
        boolean q = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String esc = v.replace("\"", "\"\"");
        return q ? "\"" + esc + "\"" : esc;
    }
    private static String csvNullableStr(String v) { return csvStr(v); }
    private static String csvLong(long v) { return String.valueOf(v); }
}

