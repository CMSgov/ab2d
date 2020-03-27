package gov.cms.ab2d.eventlogger;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class FileEventLogger implements LoggableEvent {
    public enum FileStatus {
        OPEN,
        CLOSE,
        DELETE
    }
    // id
    private Long id;
    // The file name involved
    private String fileName;
    // The job it is associated with
    private String jobId;
    // The new of the file
    private FileStatus status;
    // The size of the file if we're closing it
    private long fileSize;
    // The content hash so we can compare different files
    private String fileHash;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
