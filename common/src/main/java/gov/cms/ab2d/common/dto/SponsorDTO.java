package gov.cms.ab2d.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SponsorDTO {

    @NotNull
    private Integer hpmsId;

    @NotNull
    private String orgName;
}
