package gov.cms.ab2d.worker.model;


import gov.cms.ab2d.common.model.TimestampBase;
import gov.cms.ab2d.common.util.DateUtil;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractWorkerDto extends TimestampBase {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:m:s Z");

    public enum UpdateMode { AUTOMATIC, NONE, MANUAL }
    public enum ContractType {
        NORMAL, CLASSIC_TEST, SYNTHEA;
    }

    @NotNull
    private Long id;

    @NotNull
    private String contractNumber;

    private String contractName;

    private UpdateMode updateMode = UpdateMode.AUTOMATIC;

    private ContractType contractType = ContractType.NORMAL;

    private OffsetDateTime attestedOn;

    public boolean hasAttestation() {
        return attestedOn != null;
    }

    /**
     * Get time zone in EST time which is the standard for CMS
     */
    public ZonedDateTime getESTAttestationTime() {
        return hasAttestation() ? attestedOn.atZoneSameInstant(DateUtil.AB2D_ZONE) : null;
    }

    public boolean hasDateIssue() {
        return ContractType.CLASSIC_TEST == contractType;
    }
}
