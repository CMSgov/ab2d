package gov.cms.ab2d.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

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
