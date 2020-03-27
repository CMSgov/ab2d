package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
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
}
