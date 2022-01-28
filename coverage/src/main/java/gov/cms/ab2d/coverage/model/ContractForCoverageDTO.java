package gov.cms.ab2d.coverage.model;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import static gov.cms.ab2d.coverage.repository.CoverageServiceRepository.AB2D_ZONE;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractForCoverageDTO {

    public enum ContractType { NORMAL, CLASSIC_TEST, SYNTHEA }

    private String contractNumber;
    private OffsetDateTime attestedOn;
    private ContractType contractType;

    private boolean hasAttestation() {
        return attestedOn != null;
    }

    public ZonedDateTime getESTAttestationTime() {
        return hasAttestation() ? attestedOn.atZoneSameInstant(AB2D_ZONE) : null;
    }
}
