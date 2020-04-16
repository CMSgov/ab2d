package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.User;

public interface UserService {

    User getCurrentUser();

    User getUserByUsername(String username);

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(UserDTO userDTO);

    UserDTO enableUser(String username);

    UserDTO disableUser(String username);
}
