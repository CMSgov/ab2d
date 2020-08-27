package gov.cms.ab2d.hpms.hmsapi;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class ContractHolder {

    @NotNull
    private Set<HPMSAttestation> contracts;
}
