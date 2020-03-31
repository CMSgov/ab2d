package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Log events that happen to files such as when they are open, closed or deleted
 */
@Data
@Slf4j
public class FileEvent extends LoggableEvent {
    public enum FileStatus {
        OPEN,
        CLOSE,
        DELETE
    }
    // The file name involved
    private String fileName;
    // The new of the file
    private FileStatus status;
    // The size of the file if we're closing it
    private long fileSize;
    // The content hash so we can compare different files
    private String fileHash;

    public FileEvent(String user, String jobId, File file, FileStatus status) {
        super(OffsetDateTime.now(), user, jobId);
        this.fileName = file.getName();
        this.status = status;
        this.fileSize = file.length();
        this.fileHash = generateChecksum(file);
    }

    private String generateChecksum(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return Hex.encodeHexString(DigestUtils.sha256(fileInputStream));
        } catch (IOException e) {
            return "";
        }
    }
}
