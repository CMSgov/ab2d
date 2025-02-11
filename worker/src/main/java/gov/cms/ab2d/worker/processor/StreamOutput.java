package gov.cms.ab2d.worker.processor;


import gov.cms.ab2d.aggregator.FileOutputType;
import gov.cms.ab2d.common.util.GzipCompressUtils;
import lombok.Getter;
import lombok.val;
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
public class StreamOutput {

    /**
     * Filename of compressed file (includes the '.gz' file suffix)
     */
    private final String filePath;
    /**
     * Checksum of the uncompressed file
     */
    private final String checksum;
    /**
     * Length of the uncompressed file
     */
    private final long fileLength;

    private final FileOutputType type;

    public StreamOutput(final File uncompressedFile) {
        // calculate checksum and file length before compressing file
        this.fileLength = uncompressedFile.length();
        this.checksum = generateChecksum(uncompressedFile);

        // compress file and if successful, delete original file
        val compressedFile = GzipCompressUtils.compressFile(uncompressedFile, true);
        if (compressedFile != null) {
            this.filePath = compressedFile.getName();
            this.type = FileOutputType.getFileType(compressedFile);
        } else {
            this.filePath = uncompressedFile.getName();
            this.type = FileOutputType.getFileType(uncompressedFile);
        }
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
