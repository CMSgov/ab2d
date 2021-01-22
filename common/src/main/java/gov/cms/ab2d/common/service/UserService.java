package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.User;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService {

    User getCurrentUser();

    User getUserByUsername(String username);

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(UserDTO userDTO);

    void setupUserImpersonation(String username, HttpServletRequest request);

    void setupUserAndRolesInSecurityContext(User user, HttpServletRequest request);

    List<GrantedAuthority> getGrantedAuth(User user);

    UserDTO enableUser(String contractNumber);

    UserDTO disableUser(String contractNumber);

    UserDTO getUser(String contractNumber);
}
