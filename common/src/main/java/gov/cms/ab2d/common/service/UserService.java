package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.User;

public interface UserService {

    User getCurrentUser();

    User getUserByUsername(String username);

    User createUser(UserDTO userDTO);

    User updateUser(UserDTO userDTO);
}
