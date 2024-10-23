package gov.cms.ab2d.common.dto;

import gov.cms.ab2d.contracts.model.ContractDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PdpClientDTO {

    private Long id;

    @NotNull
    private String clientId;

    @NotNull
    private String organization;

    @NotNull
    private Boolean enabled;

    private String role;

    private ContractDTO contract;
}
