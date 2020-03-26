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

    private String fileName;
    private String jobId;
    private FileStatus status;
    private long fileSize;
    private String fileHash;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
