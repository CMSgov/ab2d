package gov.cms.ab2d.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean enabled;

    private String sponsorId;
}
