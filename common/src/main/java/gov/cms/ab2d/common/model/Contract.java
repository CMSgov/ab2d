package gov.cms.ab2d.common.model;


import gov.cms.ab2d.common.util.DateUtil;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import static gov.cms.ab2d.common.util.DateUtil.getESTOffset;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Contract extends TimestampBase {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:m:s Z");

    public enum UpdateMode { AUTOMATIC, NONE, MANUAL }
    public enum ContractType {
        NORMAL, CLASSIC_TEST, SYNTHEA;

        public boolean isTestContract() {
            return this == CLASSIC_TEST || this == SYNTHEA;
        }
    }

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    @NotNull
    private String contractNumber;

    private String contractName;

    @Column(name = "hpms_parent_org_id")
    private Long hpmsParentOrgId;

    @Column(name = "hpms_parent_org_name")
    private String hpmsParentOrg;

    @Column(name = "hpms_org_marketing_name")
    private String hpmsOrgMarketingName;

    @Enumerated(EnumType.STRING)
    private UpdateMode updateMode = UpdateMode.AUTOMATIC;

    @Enumerated(EnumType.STRING)
    private ContractType contractType = ContractType.NORMAL;

    public Contract(@NotNull String contractNumber, String contractName, Long hpmsParentOrgId, String hpmsParentOrg,
                    String hpmsOrgMarketingName) {
        this.contractNumber = contractNumber;
        this.contractName = contractName;
        this.hpmsParentOrgId = hpmsParentOrgId;
        this.hpmsParentOrg = hpmsParentOrg;
        this.hpmsOrgMarketingName = hpmsOrgMarketingName;
    }

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime attestedOn;

    public boolean isTestContract() {
        return contractType.isTestContract();
    }

    public boolean hasAttestation() {
        return attestedOn != null;
    }

    public void clearAttestation() {
        attestedOn = null;
    }

    public boolean hasChanges(String hmpsContractName, long parentOrgId, String parentOrgName, String orgMarketingName) {
        boolean allEqual = Objects.equals(hmpsContractName, contractName) &&
                        Objects.equals(parentOrgId, hpmsParentOrgId) &&
                        Objects.equals(parentOrgName, hpmsParentOrg) &&
                        Objects.equals(orgMarketingName, hpmsOrgMarketingName);

        return !allEqual;
    }

    public Contract updateOrg(String hmpsContractName, long parentOrgId, String parentOrgName, String orgMarketingName) {
        contractName = hmpsContractName;
        hpmsParentOrgId = parentOrgId;
        hpmsParentOrg = parentOrgName;
        hpmsOrgMarketingName = orgMarketingName;
        return this;
    }

    /**
     * Get time zone in EST time which is the standard for CMS
     */
    public ZonedDateTime getESTAttestationTime() {
        return hasAttestation() ? attestedOn.atZoneSameInstant(DateUtil.AB2D_ZONE) : null;
    }

    /*
     * Returns true if new state differs from existing which requires a save.
     */
    public boolean updateAttestation(boolean attested, String attestationDate) {
        if (!isAutoUpdatable())
            return false;

        boolean hasAttestation = hasAttestation();
        if (attested == hasAttestation) {
            return false;   // No changes needed
        }

        if (hasAttestation) {
            clearAttestation();
            return true;
        }

        String dateWithTZ = attestationDate + " " + getESTOffset();
        attestedOn = OffsetDateTime.parse(dateWithTZ, FORMATTER);
        return true;
    }

    public boolean isAutoUpdatable() {
        return updateMode == UpdateMode.AUTOMATIC;
    }

    public boolean hasDateIssue() {
        return ContractType.CLASSIC_TEST == contractType;
    }
}
