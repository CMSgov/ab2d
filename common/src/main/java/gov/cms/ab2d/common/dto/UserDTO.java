package gov.cms.ab2d.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class UserDTO {

    @NotNull
    private String username;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @NotNull
    private String email;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Long sponsorId;

    private Set<RoleDTO> roles;
}
