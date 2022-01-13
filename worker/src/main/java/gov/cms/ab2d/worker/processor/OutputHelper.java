package gov.cms.ab2d.worker.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Contains the common methods for other StreamHelper implementations
 */
@Slf4j
@SuppressWarnings("checkstyle:visibilitymodifier")
public class OutputHelper  {
    public enum FileOutputType {
        NDJSON(".ndjson"),
        NDJSON_ERROR("_error.ndjson"),
        ZIP(".zip");

        private String suffix;
        FileOutputType(String suffix) {
            this.suffix = suffix;
        }
        public String getSuffix() {
            return suffix;
        }
    }

    public static StreamOutput createStreamOutput(File file, boolean error) {
        String checksum = generateChecksum(file);
        return new StreamOutput(file.getName(), checksum, file.length(), error);
    }

    private static String generateChecksum(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = DigestUtils.sha256(fileInputStream);
            return Hex.encodeHexString(bytes);
        } catch (IOException e) {
            log.error("Encountered IO Exception while generating checksum {}", e.getMessage(), e);
            throw new UncheckedIOException(e);
        }
    }
}