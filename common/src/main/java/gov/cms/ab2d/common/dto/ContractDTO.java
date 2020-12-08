package gov.cms.ab2d.common.dto;

import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {

    @NotNull
    private String contractNumber;

    @NotNull
    private String contractName;

    private String attestedOn;
}
