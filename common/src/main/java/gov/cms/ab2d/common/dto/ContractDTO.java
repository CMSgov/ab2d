package gov.cms.ab2d.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ContractDTO {

    @NotNull
    private String contractNumber;

    @NotNull
    private String contractName;

    @NotNull
    private SponsorDTO sponsor;

    private String attestedOn;
}
