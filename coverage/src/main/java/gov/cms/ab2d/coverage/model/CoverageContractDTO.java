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
public class CoverageContractDTO {
    private String contractNumber;
    private OffsetDateTime attestedOn;

    public boolean hasAttestation() {
        return attestedOn != null;
    }

    public ZonedDateTime getESTAttestationTime() {
        return hasAttestation() ? attestedOn.atZoneSameInstant(AB2D_ZONE) : null;
    }
}
