package gov.cms.ab2d.contracts.model;

import gov.cms.ab2d.contracts.utils.DateUtil;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static gov.cms.ab2d.contracts.utils.DateUtil.getESTOffset;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "contract", schema = "contract")
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_seq")
    @SequenceGenerator(name = "contract_seq", sequenceName = "contract_seq", schema = "contract", allocationSize = 1)
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

    @Column(name = "total_enrollment")
    private Integer totalEnrollment;

    @Column(name = "medicare_eligible")
    private Integer medicareEligible;

    public Contract(@NotNull String contractNumber, String contractName, Long hpmsParentOrgId, String hpmsParentOrg,
                    String hpmsOrgMarketingName, Integer totalEnrollment, Integer medicareEligible) {
        this.contractNumber = contractNumber;
        this.contractName = contractName;
        this.hpmsParentOrgId = hpmsParentOrgId;
        this.hpmsParentOrg = hpmsParentOrg;
        this.hpmsOrgMarketingName = hpmsOrgMarketingName;
        this.totalEnrollment = totalEnrollment;
        this.medicareEligible = medicareEligible;
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

    public boolean hasChanges(String hmpsContractName, long parentOrgId, String parentOrgName, String orgMarketingName,
                              Integer hpmsTotalEnrollment, Integer hpmsMedicareEligible) {
        boolean allEqual = Objects.equals(hmpsContractName, contractName) &&
                Objects.equals(parentOrgId, hpmsParentOrgId) &&
                Objects.equals(parentOrgName, hpmsParentOrg) &&
                Objects.equals(orgMarketingName, hpmsOrgMarketingName) &&
                Objects.equals(totalEnrollment, hpmsTotalEnrollment) &&
                Objects.equals(medicareEligible, hpmsMedicareEligible);

        return !allEqual;
    }

    public Contract updateOrg(String hmpsContractName, long parentOrgId, String parentOrgName, String orgMarketingName,
                              Integer hpmsTotalEnrollment, Integer hpmsMedicareEligible) {
        this.contractName = hmpsContractName;
        this.hpmsParentOrgId = parentOrgId;
        this.hpmsParentOrg = parentOrgName;
        this.hpmsOrgMarketingName = orgMarketingName;
        this.totalEnrollment = hpmsTotalEnrollment;
        this.medicareEligible = hpmsMedicareEligible;
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

    public ContractDTO toDTO() {
        return new ContractDTO(getId(), getContractNumber(), getContractName(),
                getAttestedOn(), getContractType(), getTotalEnrollment(), getMedicareEligible());
    }
}
