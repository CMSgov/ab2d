package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Records when a bulk import is done of beneficiary data
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReloadEvent extends LoggableEvent {
    public enum FileType {
        OPT_OUT,
        CONTRACT_MAPPING,
        UPLOAD_ORG_STRUCTURE_REPORT,
        ATTESTATION_REPORT,
        PROPERTIES
    }
    // The type of load being performed
    private FileType fileType;
    // The file name we're loading from
    private String fileName;
    // The number of values loaded
    private int numberLoaded;

    public ReloadEvent() { }

    public ReloadEvent(String organization, FileType fileType, String fileName, int numLoaded) {
        super(OffsetDateTime.now(), organization, null);
        this.fileType = fileType;
        this.fileName = fileName;
        this.numberLoaded = numLoaded;
    }

    @Override
    public String asMessage() {
        return String.format("(%s) %s %s", getJobId(), fileType, fileName);
    }
}
