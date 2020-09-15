package gov.cms.ab2d.hpms.hmsapi;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class HPMSAttestation {

    private String contractId;
    private boolean attested;
    private String attestationDate;
}
