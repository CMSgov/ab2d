package gov.cms.ab2d.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class UserDTO {

    private Long id;

    @NotNull
    private String username;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    private String email;

    @NotNull
    private Boolean enabled;

    @NotNull
    private SponsorDTO sponsor;

    private String role;

    private Set<ContractDTO> contracts;
}
