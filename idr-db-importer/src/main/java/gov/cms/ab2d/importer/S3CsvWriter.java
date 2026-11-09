package gov.cms.ab2d.importer;

import datadog.trace.api.Trace;
import gov.cms.ab2d.common.util.DatadogSpans;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
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

    private static final int PART_BYTES = 8 * 1024 * 1024;

    @Trace(operationName = "ab2d.idr.export_to_s3")
    public void writeSnowflakeToS3(String bucket, String finalKey, ResultSet rs) throws Exception {
        DatadogSpans.setTag("component", "idr");
        DatadogSpans.setTag("s3.bucket", bucket);
        DatadogSpans.setTag("s3.key", finalKey);
        String uploadId = s3.createMultipartUpload(
                r -> r.bucket(bucket).key(finalKey).contentType("text/csv")
        ).uploadId();

        List<CompletedPart> parts = new ArrayList<>();
        int partNum = 1;

        ByteArrayOutputStream buf = new ByteArrayOutputStream(PART_BYTES);

        try {
            while (rs.next()) {
                String line = String.join(",",
                        csvLong(rs.getLong(1)),
                        csvStr(rs.getString(2)),
                        String.valueOf(rs.getInt(3)),
                        String.valueOf(rs.getInt(4)),
                        csvStr(rs.getString(5))
                );
                writeLine(buf, line);

                if (buf.size() >= PART_BYTES) {
                    parts.add(uploadPart(bucket, finalKey, uploadId, partNum++, buf.toByteArray()));
                    buf.reset();
                }
            }

            if (buf.size() > 0) {
                parts.add(uploadPart(bucket, finalKey, uploadId, partNum, buf.toByteArray()));
            }

            if (parts.isEmpty()) {
                throw new IllegalStateException("Snowflake returned no rows for s3://" + bucket + "/" + finalKey);
            }

            s3.completeMultipartUpload(r -> r.bucket(bucket).key(finalKey).uploadId(uploadId)
                    .multipartUpload(m -> m.parts(parts)));

        } catch (Exception e) {
            DatadogSpans.markError(e);
            log.warn("Aborting multipart upload uploadId={} for s3://{}/{}", uploadId, bucket, finalKey, e);
            abort(bucket, finalKey, uploadId);
            throw e;
        }
    }

    private void abort(String bucket, String key, String uploadId) {
        try {
            s3.abortMultipartUpload(r -> r.bucket(bucket).key(key).uploadId(uploadId));
        } catch (NoSuchUploadException ex) {
            log.warn("Multipart upload already gone during abort for s3://{}/{} uploadId={}", bucket, key, uploadId);
        } catch (Exception ex) {
            log.warn("Failed to abort multipart upload uploadId={} for s3://{}/{}", uploadId, bucket, key, ex);
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

    private static String csvLong(long v) {
        return String.valueOf(v);
    }
}