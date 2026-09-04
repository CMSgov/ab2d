package gov.cms.ab2d.contracts.hmsapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor  // Needed for Jackson
@AllArgsConstructor
@Data
public class HPMSAttestation {

    private String contractId;
    private boolean attested;
    private String attestationDate;
    private String endDate;

    // HPMS contract lifecycle status, e.g. whether the contract is currently active. Left as the
    // raw upstream string so an unrecognised value is stored rather than dropped.
    private String contractStatus;
}
