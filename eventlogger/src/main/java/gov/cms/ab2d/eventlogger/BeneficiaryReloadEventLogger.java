package gov.cms.ab2d.eventlogger;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Records when a bulk import is done of beneficiary data
 */
@Data
public class BeneficiaryReloadEventLogger implements LoggableEvent {
    public enum FileType {
        OPT_OUT,
        CONTRACT_MAPPING
    }
    // The type of load being performed
    private FileType fileType;
    // The file name we're loading from
    private String fileName;
    // The number of values loaded
    private int numberLoaded;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
