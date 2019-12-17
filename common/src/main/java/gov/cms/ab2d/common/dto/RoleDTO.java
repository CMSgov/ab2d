package gov.cms.ab2d.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RoleDTO {

    @NotNull
    private Integer id;

    @NotNull
    private String name;
}
