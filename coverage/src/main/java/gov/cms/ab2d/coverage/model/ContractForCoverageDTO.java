package gov.cms.ab2d.coverage.model;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import static gov.cms.ab2d.coverage.repository.CoverageServiceRepository.AB2D_ZONE;

@Entity(name = "contract")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractForCoverageDTO {

    public enum ContractType {NORMAL, CLASSIC_TEST, SYNTHEA}

    @Id
    private String contractNumber;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime attestedOn;

    @Enumerated(EnumType.STRING)
    private ContractType contractType = ContractType.NORMAL;

    public boolean hasAttestation() {
        return attestedOn != null;
    }

    public ZonedDateTime getESTAttestationTime() {
        return hasAttestation() ? attestedOn.atZoneSameInstant(AB2D_ZONE) : null;
    }
}
