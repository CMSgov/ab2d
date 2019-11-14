package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
/**
 * Just gets the current user from the authentication context.
 */
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? userRepository.findByUserName(
                ((org.springframework.security.core.userdetails.User) auth.getPrincipal())
                        .getUsername()) : null;
    }

}
