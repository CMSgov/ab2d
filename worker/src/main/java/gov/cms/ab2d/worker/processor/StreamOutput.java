package gov.cms.ab2d.worker.processor;


import gov.cms.ab2d.aggregator.FileOutputType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A single file output from a stream implementation with the necessary details
 * to identify the file
 */
@Getter
@AllArgsConstructor
public class StreamOutput {

    private final String filePath;
    private final String checksum;
    private final long fileLength;
    private final FileOutputType type;

    public StreamOutput(File file, FileOutputType type) {
        this.filePath = file.getName();
        this.type = type;
        this.fileLength = file.length();
        this.checksum = generateChecksum(file);
    }

    public static String generateChecksum(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = DigestUtils.sha256(fileInputStream);
            return Hex.encodeHexString(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Encountered IO Exception while generating checksum {}", e);
        }
    }
}
