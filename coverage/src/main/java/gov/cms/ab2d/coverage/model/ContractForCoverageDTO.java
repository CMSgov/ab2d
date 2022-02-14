package gov.cms.ab2d.coverage.model;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractForCoverageDTO {

    private static final int SYNTHETIC_DATA_YEAR = 3;

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

    public int getCorrectedYear(int coverageYear) {

        // Synthea contracts use realistic enrollment reference years so only original
        // synthetic contracts need to have the year modified
        if (getContractType() == ContractType.CLASSIC_TEST) {
            return SYNTHETIC_DATA_YEAR;
        }

        return coverageYear;
    }
}
