package gov.cms.ab2d.hpms.hmsapi;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@AllArgsConstructor
@Data
public class HPMSAttestationsHolder {

    @NotNull
    private Set<HPMSAttestation> contracts;
}
