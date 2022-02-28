package gov.cms.ab2d.worker.model;


import gov.cms.ab2d.common.model.TimestampBase;
import gov.cms.ab2d.common.util.DateUtil;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ContractWorkerDto extends TimestampBase {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:m:s Z");

    public enum UpdateMode { AUTOMATIC, NONE, MANUAL }
    public enum ContractType {
        NORMAL, CLASSIC_TEST, SYNTHEA;
    }

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    @NotNull
    private String contractNumber;

    private String contractName;

    @Enumerated(EnumType.STRING)
    private UpdateMode updateMode = UpdateMode.AUTOMATIC;

    @Enumerated(EnumType.STRING)
    private ContractType contractType = ContractType.NORMAL;

    public ContractWorkerDto(@NotNull String contractNumber, String contractName) {
        this.contractNumber = contractNumber;
        this.contractName = contractName;
    }

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime attestedOn;

    public boolean hasAttestation() {
        return attestedOn != null;
    }

    public void clearAttestation() {
        attestedOn = null;
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
